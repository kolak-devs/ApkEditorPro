package com.mcal.apkeditor.activities

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import com.mcal.apkeditor.ApkComposeService
import com.mcal.apkeditor.ApkComposeService.ComposeServiceBinder
import com.mcal.apkeditor.R
import com.mcal.apkeditor.adapters.ApkComposeFailAdapter
import com.mcal.apkeditor.ce.IApkMaking
import com.mcal.apkeditor.data.Constants
import com.mcal.apkeditor.databinding.ActivityApkcomposeBinding
import com.mcal.apkeditor.utils.AxmlStringModifier
import com.mcal.apkeditor.utils.ErrorFixManager
import com.mcal.apkeditor.utils.OdexPatcher
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.utils.ApkInfoParser
import com.mcal.common.utils.ApkInstaller
import com.mcal.common.utils.ClipboardUtils.copyToClipboard
import com.mcal.common.utils.ITaskCallback
import com.mcal.common.utils.ITaskCallback.TaskStepInfo
import com.mcal.common.utils.PackageHelper.uninstallPackage
import com.mcal.common.view.ProgressDialog
import com.mcal.common.view.ProgressDialog.ProcessingInterface
import org.jetbrains.annotations.Contract
import java.io.File
import java.lang.ref.WeakReference

class ApkComposeActivity : CustomizedLangActivity(), ITaskCallback, View.OnClickListener {
    private lateinit var binding: ActivityApkcomposeBinding

    // Created from notification or not (by clicking at notification)
    var createdFromNotification = false

    // Generated apk path, Package name of the apk file
    @JvmField
    var srcApkPath: String? = null

    // Apply patch to code cache succeed or not
    private var patchSucceed = false

    // Progressing and result
    private var msgHandler: MyHandler = MyHandler(this)
    private var stepInfo: TaskStepInfo? = null
    private var errMessage: String? = null
    private var targetApkPath: String? = null
    private var mPackageName: String? = null
    private var decodeRootPath: String? = null
    private var codeModified = false
    private var signAPK = false
    private var mBinder: ComposeServiceBinder? = null

    // Use to automatically fix the error
    private var errFixer: ErrorFixManager? = null

    // Activity visible or not
    private var isActivityVisible = false

    // To different the invoke from service
    private var intentAction: String? = null
    private var connection: ServiceConnection? = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ComposeServiceBinder
            mBinder = binder

            // Cancel the notification if invoked from service
            // When activity is created by clicking at the notification, will into following code
            intentAction?.let { intent ->
                if (Constants.ACTION.MAIN_ACTION == intent) {
                    createdFromNotification = true
                    if (!binder.isRunning()) {
                        binder.hideNotification()
                    }
                    // Show "Put to Background" button
                    binding.btnBg.visibility = View.VISIBLE
                }
            }
            binder.setObserver(this@ApkComposeActivity)
            val keyValues = binder.values
            srcApkPath = keyValues["srcApkPath"] as String?
            Log.wtf("SVolf", "get value srcApkPath = $srcApkPath")
            targetApkPath = keyValues["targetApkPath"] as String?
            Log.wtf("SVolf", "get value srcApkPath = $targetApkPath")
            decodeRootPath = keyValues["decodeRootPath"] as String?
            Log.wtf("SVolf", "get value srcApkPath = $decodeRootPath")
            codeModified = (keyValues["codeModified"] as Boolean?)!!
            Log.wtf("SVolf", "get value srcApkPath = $codeModified")
            signAPK = (keyValues["signAPK"] as Boolean?)!!
            Log.wtf("SVolf", "get value srcApkPath = $signAPK")
            errFixer = ErrorFixManager(decodeRootPath)
        }

        override fun onServiceDisconnected(name: ComponentName) {}
    }

    private fun createChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chan1 = NotificationChannel(PRIMARY_NOTIFY_CHANNEL, "default", NotificationManager.IMPORTANCE_LOW)
        chan1.lightColor = Color.TRANSPARENT
        chan1.enableVibration(false)
        chan1.vibrationPattern = longArrayOf(0L)
        chan1.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(chan1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Create notification channel
        createChannel()
        intentAction = intent.action
        binding = ActivityApkcomposeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.result.setOnClickListener(this)
        switchView(true)

        // Close button
        binding.btnClose.setOnClickListener(this)

        // Remove the old app
        binding.btnRemove.setOnClickListener(this)

        // Fix the issue
        binding.btnFix.setOnClickListener(this)

        // Copy error message
        binding.btnCopyErrmsg.setOnClickListener(this)

        // Put it to background
        binding.btnBg.setOnClickListener(this)

        // Запуск сервиса
        connection?.let { conn ->
            bindService(Intent(this, ApkComposeService::class.java), conn, Context.BIND_AUTO_CREATE)
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentAction = intent.action
        // Cancel the notification if invoked from service
        if (Constants.ACTION.MAIN_ACTION == intentAction) {
            mBinder?.let { binder ->
                if (!binder.isRunning()) {
                    binder.hideNotification()
                }
            }
        }
    }

    public override fun onPause() {
        isActivityVisible = false
        super.onPause()
    }

    override fun onDestroy() {
        connection?.let { conn ->
            unbindService(conn)
            connection = null
        }
        stopBuildAndGoBack()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
    }

    override fun setTaskStepInfo(stepInfo: TaskStepInfo) {
        this.stepInfo = stepInfo
        msgHandler.sendEmptyMessage(MyHandler.STEP_INFO)
    }

    override fun setTaskProgress(progress: Float) {
        // progress not returned yet
    }

    override fun taskSucceed() {
        msgHandler.sendEmptyMessage(MyHandler.SUCCEED)
    }

    override fun taskFailed(errMessage: String) {
        this.errMessage = errMessage
        msgHandler.sendEmptyMessage(MyHandler.FAILED)
    }

    override fun taskWarning(message: String) {
        msgHandler.toast(message)
    }

    fun updateComposeInfo() {
        stepInfo?.let { info ->
            binding.progressTip.text = String.format(
                resources.getString(R.string.step) + " %d/%d: %s",
                info.stepIndex, info.stepTotal,
                info.stepDescription
            )
        }
    }

    fun composeFinished(ret: Boolean) {
        switchView(false)
        // Clear notification in status bar when activity not finished
        mBinder?.let { binder ->
            if (isActivityVisible) {
                binder.hideNotification()
            }
        }
        if (ret) {
            this.setResult(SUCCEED)
            binding.btnInstall.setOnClickListener(this)

            // Hide the failed view
            binding.succeededView.visibility = View.VISIBLE
            binding.failedView.visibility = View.GONE

            // Show succeed image
            binding.resultImage.setImageResource(R.drawable.round_done_green_24)
            val str = resources.getString(R.string.apk_savedas_1)
            val strPlace = String.format(str, targetApkPath)
            val strSucceed = resources.getString(R.string.succeed)
            val message = "$strSucceed!\n$strPlace\n\n"

            // APK is signed
            if (signAPK) {
                // Check if already installed
                mPackageName = apkPackageName
                mPackageName?.let { pkg ->
                    if (isPackageInstalled(pkg)) {
                        val allStr = message + resources.getString(R.string.remove_tip)
                        val style = SpannableStringBuilder(allStr)
                        style.setSpan(
                            AbsoluteSizeSpan(22), message.length,
                            allStr.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        binding.result.text = style
                        binding.btnRemove.visibility = View.VISIBLE
                    } else {
                        binding.result.text = message
                        binding.btnRemove.visibility = View.GONE
                    }
                }
                binding.btnCopyErrmsg.visibility = View.GONE
                binding.btnInstall.visibility = View.VISIBLE
            } else {
                val allStr = message + resources.getString(R.string.not_signed_tip)
                val style = SpannableStringBuilder(allStr)
                style.setSpan(
                    AbsoluteSizeSpan(22), message.length,
                    allStr.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                val fcs = ForegroundColorSpan(Color.rgb(255, 30, 30))
                style.setSpan(
                    fcs, message.length,
                    allStr.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                binding.result.text = style
                binding.btnCopyErrmsg.visibility = View.GONE
                binding.btnRemove.visibility = View.GONE
                binding.btnInstall.visibility = View.GONE
            }

            if (codeModified && isArtRuntime) {
                binding.patchDexLayout.visibility = View.VISIBLE
                binding.btnPatch.setOnClickListener(this)
            } else {
                binding.patchDexLayout.visibility = View.GONE
            }
        } else {
            this.setResult(FAILED)

            // Hide the succeed view
            binding.succeededView.visibility = View.GONE
            binding.failedView.visibility = View.VISIBLE
            errMessage?.let { message ->
                binding.failedView.adapter = ApkComposeFailAdapter(this, message)
                Log.d("error", message)
            }
            binding.resultImage.setImageResource(R.drawable.round_close_red_24)

            // Auto fix
            errMessage?.let { message ->
                errFixer?.let { fixer ->
                    fixer.setErrMessage(message)
                    if (fixer.isErrorFixable) {
                        val resId = tipResourceId
                        binding.tvFixTip.setText(resId)
                        binding.fixLayout.visibility = View.VISIBLE
                    }
                }
            }

            // Update button
            binding.btnInstall.visibility = View.GONE
            binding.btnRemove.visibility = View.GONE
            binding.btnCopyErrmsg.visibility = View.VISIBLE
        }
    }

    private val isArtRuntime: Boolean
        get() {
            val vmVersion = System.getProperty("java.vm.version")
            return vmVersion != null && vmVersion[0] > '1'
        }
    private val tipResourceId: Int
        get() {
            val fid = errFixer?.fixerId
            var resId = -1
            when (fid) {
                ErrorFixManager.FIXER_INVALID_FILENAME -> resId = R.string.fix_invalid_name_tip
                ErrorFixManager.FIXER_INVALID_TOKEN -> resId = R.string.fix_invalid_token_tip
                ErrorFixManager.FIXER_INVALID_ATTR -> resId = R.string.fix_invalid_attr_tip
                ErrorFixManager.FIXER_INVALID_SYMBOL -> resId = R.string.fix_invalid_symbol_tip
                ErrorFixManager.FIXER_ERROR_EQUIVALENT -> resId = R.string.fix_error_equivalent
            }
            return resId
        }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Get the package name of the new build apk file
    private val apkPackageName: String?
        get() {
            try {
                ApkInfoParser().parse(this, targetApkPath)?.let { info ->
                    return info.pkgName
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_close -> {
                finish()
            }
            R.id.btn_install -> {
                ApkInstaller.install(this, targetApkPath)
            }
            R.id.btn_remove -> {
                mPackageName?.let { pkg ->
                    uninstallPackage(this, pkg)
                }
            }
            R.id.btn_copy_errmsg -> {
                copyToClipboard(this, errMessage)
                Toast.makeText(this, R.string.errmsg_copied, Toast.LENGTH_SHORT).show()
            }
            R.id.btn_fix -> {
                errFixer?.let { fixer ->
                    binding.fixLayout.visibility = View.GONE
                    fixer.fixErrors(this)
                }
            }
            R.id.btn_patch -> {
                if (!patchSucceed) {
                    applyCodePatch()
                } else {
                    launchApp()
                }
            }
            R.id.btn_bg -> {
                finish()
            }
            R.id.result -> {
                targetApkPath?.let { path ->
                    showFileInExplorer(path)
                }
            }
        }
    }

    private fun showFileInExplorer(filepath: String) {
        if (!File(filepath).exists()) {
            return
        }
        val position = filepath.lastIndexOf("/")
        if (position == -1) {
            return
        }
        val path = filepath.substring(0, position + 1)
        val file = File(path)
        if (!file.exists()) {
            return
        }
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setDataAndType(Uri.fromFile(file), "text/csv")
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun launchApp() {
        val pm = this.packageManager
        try {
            val it = pm.getLaunchIntentForPackage(mPackageName!!)
            if (null != it) {
                this.startActivity(it)
            }
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    // Apply the DEX patch to the cache
    private fun applyCodePatch() {
        ProgressDialog(
            this, "", "Working…", false,
            object : ProcessingInterface {
                private var errMessage: String? = null
                private var targetOdex: String? = null

                @Throws(Exception::class)
                override fun process() {
                    val patcher = OdexPatcher(mPackageName)
                    patcher.applyPatch(this@ApkComposeActivity, targetApkPath)
                    targetOdex = patcher.targetOdex
                    if (patcher.errMessage != null) {
                        errMessage = patcher.errMessage
                        throw Exception(errMessage)
                    }
                }

                override fun afterProcess() {
                    if (errMessage == null) {
                        patchSucceed = true
                        val fmt = this@ApkComposeActivity.getString(R.string.patch_code_cache_done)
                        val msg = String.format(fmt, targetOdex)
                        binding.tvPatchTip.text = msg
                        binding.btnPatch.setText(R.string.launch)
                    } else {
                        binding.tvPatchTip.text = errMessage
                    }
                }
            }, -1
        ).show()
    }

    fun buildAgain() {
        mBinder?.let { binder ->
            // Set extra AXML Modifier
            val m = errFixer?.modifications
            if (!m.isNullOrEmpty()) {
                createBuildHooker(m)?.let { hooker ->
                    binder.setBuildHooker(hooker)
                }
            }

            // Switch the layout and build again
            binding.progressTip.text = ""
            switchView(true)
            binder.buildAgain()
        }
    }

    @Contract(value = "_ -> new", pure = true)
    private fun createBuildHooker(
        modifications: Map<String, Map<String, String>>
    ): IApkMaking? {
        decodeRootPath?.let { path ->
            return AxmlStringModifier(path, modifications)
        }
        return null
    }


    private fun stopBuildAndGoBack() {
        try {
            mBinder?.let { binder ->
                binder.stopBuilding()
            }
            connection?.let { conn ->
                unbindService(conn)
                connection = null
            }
            val intent = Intent(this, ApkComposeService::class.java)
            stopService(intent)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun switchView(loading: Boolean) {
        if (loading) {
            binding.layoutApkComposing.visibility = View.VISIBLE
            binding.layoutApkComposed.visibility = View.GONE
        } else {
            binding.layoutApkComposing.visibility = View.GONE
            binding.layoutApkComposed.visibility = View.VISIBLE
        }
    }

    override fun onBackPressed() {
        mBinder?.let { binder ->
            if (binder.isRunning()) {
                finish()
            }
//            } else {
//                finish()
//            }
        }
    }

    private class MyHandler(a: ApkComposeActivity) : Handler() {
        private val activityRef: WeakReference<ApkComposeActivity>
        private var tmpMessage: String? = null

        init {
            activityRef = WeakReference(a)
        }

        fun toast(msg: String) {
            tmpMessage = if ("x" == msg) {
                "Manifest editing is disabled (seems not a genuine version)"
            } else {
                msg
            }
            sendEmptyMessage(SHOW_MESSAGE)
        }

        override fun handleMessage(msg: Message) {
            val activity = activityRef.get()
            if (activity != null) {
                when (msg.what) {
                    STEP_INFO -> activity.updateComposeInfo()
                    SUCCEED -> activity.composeFinished(true)
                    FAILED -> activity.composeFinished(false)
                    SHOW_MESSAGE -> Toast.makeText(activity, tmpMessage, Toast.LENGTH_LONG).show()
                    SHOW_BG_BUTTON -> activity.binding.btnBg.visibility = View.VISIBLE
                }
            }
        }

        companion object {
            const val STEP_INFO = 1
            const val SUCCEED = 2
            const val FAILED = 3
            const val SHOW_MESSAGE = 4
            const val SHOW_BG_BUTTON = 6
        }
    }

    companion object {
        const val SUCCEED = 10005
        const val FAILED = -1
        const val PRIMARY_NOTIFY_CHANNEL = "default"
    }
}