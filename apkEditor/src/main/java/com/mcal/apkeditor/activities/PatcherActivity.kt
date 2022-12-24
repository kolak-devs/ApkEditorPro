package com.mcal.apkeditor.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.IGeneralCallback
import com.mcal.apkeditor.R
import com.mcal.apkeditor.ResListAdapter
import com.mcal.apkeditor.adapters.PatcherHistoryItem
import com.mcal.apkeditor.databinding.ActivityPatcherBinding
import com.mcal.apkeditor.patch.PatchExecutor
import com.mcal.apkeditor.patch.interfaces.ApkInfoListener
import com.mcal.apkeditor.patch.interfaces.IPatchContext
import com.mcal.apkeditor.smali.AsyncDecodeTask
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.activities.WebViewActivity
import com.mcal.common.data.Constants
import com.mcal.common.filesystem.FilePickHelper
import com.mcal.common.utils.ActivityHelper
import com.mcal.common.utils.ApkInfoParser
import com.mcal.common.utils.ScopedStorage.getPatchesDir
import com.mcal.common.utils.copyFile
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import org.xml.sax.SAXException
import ru.mcal.manifestparser.xml.AndroidManifestParser
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.zip.ZipFile
import javax.xml.parsers.ParserConfigurationException

class PatcherActivity : CustomizedLangActivity(), ApkInfoListener, IPatchContext, AsyncDecodeTask.IDecodeTaskCallback {
    private var _binding: ActivityPatcherBinding? = null
    private val binding get() = _binding!!
    private lateinit var pickLauncher: ActivityResultLauncher<Intent>
    private var mDecodedPath: String? = null
    private var mApkPath: String? = null
    private var mApkInfo: ApkInfoParser.AppInfo? = null
    private var mPatchPath: String? = null
    private var mIsDexDecoded: Boolean = false
    private var mDexDecodedCallback: IGeneralCallback? = null

    // Record all the global parameter values
    private val globalVariableValues: MutableMap<String, String> = HashMap()

    // Record executor as the parse is done there
    private var patchExecutor: PatchExecutor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPatcherBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(R.id.toolbar, "Patcher", true)

        if (intent.extras != null) {
            mDecodedPath = intent.getStringExtra(DECODE_PATH)
            mApkPath = intent.getStringExtra(APK_PATH)
            mIsDexDecoded = intent.getBooleanExtra(IS_DECODED_DEX, false)
            mApkInfo = ApkInfoParser().parse(this, mApkPath)
        }

        binding.selectPatch.setOnClickListener {
            pickLauncher.launch(FilePickHelper.pickFile(false))
        }

        binding.applyPatch.setOnClickListener {
            mPatchPath?.let { path ->
                patchExecutor = PatchExecutor(this, this, path, this)
                patchExecutor?.applyPatch()
                binding.applyPatch.isEnabled = false
            }
        }

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_patcher, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.help -> {
                        val link = "${Constants.getDomain()}/apkeditor/doc/patcher/index.html"
                        val intent = Intent(this@PatcherActivity, WebViewActivity::class.java)
                        ActivityHelper.attachParam(intent, HTML_URL, link)
                        startActivity(intent)
                        return true
                    }
                    R.id.history -> {
                        val itemAdapter = ItemAdapter<PatcherHistoryItem>()
                        val fastAdapter = FastAdapter.with(itemAdapter)
                        /**
                         * Получаем список файлов в директории патчей. И отображаем на экране все архивы
                         */
                        getPatchesDir().listFiles()?.let { files ->
                            for (f in files) {
                                if (f.exists() && f.name.endsWith(".zip")) {
                                    val fmt = SimpleDateFormat("EEE, HH:mm")
                                    itemAdapter.add(
                                        PatcherHistoryItem(
                                            System.currentTimeMillis().toInt(), ContextCompat.getDrawable(this@PatcherActivity, R.drawable.ic_android), f.name,
                                            fmt.format(f.lastModified())
                                        )
                                    )
                                }
                            }
                        }

                        if (itemAdapter.adapterItemCount >= 0) {
                            LayoutInflater.from(this@PatcherActivity).inflate(R.layout.dialog_patcher_history, null).apply {
                                findViewById<RecyclerView>(R.id.recycler_view).apply {
                                    adapter = fastAdapter
                                }
                                val dialog = MaterialAlertDialogBuilder(this@PatcherActivity).create()
                                dialog.setTitle(R.string.select_patch)
                                dialog.setView(this)
                                dialog.show()

                                fastAdapter.onClickListener = { _: View?, _: IAdapter<PatcherHistoryItem>, mainMenuItem: PatcherHistoryItem, i: Int ->
                                    mainMenuItem.title?.let { title ->
                                        mPatchPath = File(getPatchesDir(), title).path
                                        binding.filename.setText(title)
                                        dialog.dismiss()
                                    }
                                    true
                                }
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })

        pickLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    /**
                     * Получаем URI на АПК и копируем его в директорию проектов для работы с этим АПК
                     */
                    val patchFile = File(getPatchesDir(), FilePickHelper.getFileName(this, uri))
                    contentResolver.openInputStream(uri)?.let { inputStream ->
                        copyFile(inputStream, patchFile)
                    }.also {
                        if (patchFile.exists()) {
                            mPatchPath = patchFile.path
                            binding.filename.setText(patchFile.name)
                        } else {
                            Toast.makeText(this@PatcherActivity, R.string.msg_unsupported_file, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        savedInstanceState?.let {
            mPatchPath = it.getString(PATCH_PATH)
            mDecodedPath = it.getString(DECODE_PATH)
            mApkPath = it.getString(APK_PATH)
            mIsDexDecoded = it.getBoolean(IS_DECODED_DEX)
            binding.log.text = it.getString(LOG)
            binding.filename.setText(it.getString(PATCH_NAME))
            mApkInfo = ApkInfoParser().parse(this, mApkPath)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PATCH_PATH, mPatchPath)
        outState.putString(DECODE_PATH, mDecodedPath)
        outState.putString(APK_PATH, mApkPath)
        outState.putBoolean(IS_DECODED_DEX, mIsDexDecoded)
        outState.putString(LOG, binding.log.text.toString())
        outState.putString(PATCH_NAME, binding.filename.text.toString())
    }

    companion object {
        const val PATCH_NAME = "name"
        const val LOG = "log"
        const val PATCH_PATH = "patchPath"
        const val HTML_URL = "htmlUrl"
        const val DECODE_PATH = "decodeRootPath"
        const val APK_PATH = "apkPath"
        const val IS_DECODED_DEX = "dex2smaliClicked"
    }


    public override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    override fun getResListAdapter(): ResListAdapter? {
        return null
    }

    override fun getDecodeRootPath(): String? = mDecodedPath

    override fun getSmaliFolders(): MutableList<String> {
        val folders: MutableList<String> = ArrayList()
        folders.add("smali")

        // Look into zip file to get all dex, thus get related smali folder
        val apkPath = mApkPath
        apkPath?.takeIf { File(it).exists() }?.let {
            var zfile: ZipFile? = null
            try {
                zfile = ZipFile(apkPath)
                val entries = zfile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (name.endsWith(".dex") && !name.contains("/")) {
                        if (name != "classes.dex") {
                            folders.add(
                                "smali_" + name.substring(0, name.length - 4)
                            )
                        }
                    }
                }
            } catch (e1: IOException) {
                e1.printStackTrace()
            } finally {
                zfile?.close()
            }
        }

        return folders
    }

    override fun getApplicationName(): String? {
        try {
            val parser = AndroidManifestParser.parse(FileInputStream(File("$mDecodedPath/AndroidManifest.xml")))
            return parser.applicationName
        } catch (e: ParserConfigurationException) {
            e.printStackTrace()
        } catch (e: SAXException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun getActivities(): MutableList<String>? {
        val activityName: MutableList<String> = ArrayList()
        try {
            val parser = AndroidManifestParser.parse(FileInputStream(File("$mDecodedPath/AndroidManifest.xml")))
            for (activity in parser.activities) {
                activityName.add(activity.name)
            }
            @Suppress("UNNECESSARY_SAFE_CALL")
            activityName?.let {
                return activityName
            }
        } catch (e: ParserConfigurationException) {
            e.printStackTrace()
        } catch (e: SAXException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun getLauncherActivities(): MutableList<String>? {
        val activityName: MutableList<String> = ArrayList()
        try {
            val parser = AndroidManifestParser.parse(FileInputStream(File("$mDecodedPath/AndroidManifest.xml")))
            activityName.add(parser.launcherActivity.name)
            @Suppress("UNNECESSARY_SAFE_CALL")
            activityName?.let {
                return activityName
            }
        } catch (e: ParserConfigurationException) {
            e.printStackTrace()
        } catch (e: SAXException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun getPatchNames(): MutableList<String>? = patchExecutor?.ruleNames

    override fun info(resourceId: Int, bold: Boolean, vararg args: Any?) {
        var txt = getString(resourceId)
        @Suppress("UNNECESSARY_SAFE_CALL")
        args?.let {
            txt = String.format(txt, *args)
        }
        val message = if (bold) "\n" + txt + "\n" else txt + "\n"
        appendText(message, bold, false)
    }

    override fun info(format: String, bold: Boolean, vararg args: Any?) {
        var txt = format
        @Suppress("UNNECESSARY_SAFE_CALL")
        args?.let {
            txt = String.format(txt, *args)
        }
        val message = if (bold) "\n" + txt + "\n" else txt + "\n"
        appendText(message, bold, false)
    }

    override fun error(resourceId: Int, vararg args: Any?) {
        var txt = getString(resourceId)
        @Suppress("UNNECESSARY_SAFE_CALL")
        args?.let {
            txt = String.format(txt, *args)
        }
        appendText(txt + "\n", bold = false, red = true)
    }

    override fun patchFinished() {
        appendText("\nFinished", bold = true, red = false)
        runOnUiThread {
            binding.applyPatch.isEnabled = true
        }
    }

    override fun setVariableValue(key: String, value: String) {
        globalVariableValues[key] = value
    }

    override fun getVariableValue(key: String?): String? = globalVariableValues[key]

    override fun getApkInfo(): ApkInfoParser.AppInfo? = mApkInfo

    override fun getApkPath(): String? = mApkPath

    override fun setManifestModified(b: Boolean) = Unit

    override fun isDexDecoded(): Boolean {
        return mIsDexDecoded
    }

    override fun decodeDex(dexDecodedCallback: IGeneralCallback?) {
        mDexDecodedCallback = dexDecodedCallback
        mApkPath?.let { apkPath ->
            mDecodedPath?.let { decodedPath ->
                AsyncDecodeTask(apkPath, decodedPath, this).execute()
                mIsDexDecoded = true
            }
        }
    }

    override fun addLanguageRetError(strCode: String?): String = ""

    override fun translateLanguage(lang: String?) = Unit

    private fun appendText(
        txt: String, bold: Boolean,
        red: Boolean
    ) {
        runOnUiThread {
            if (red) {
                val spanString = SpannableString(txt)
                val span = ForegroundColorSpan(Color.RED)
                spanString.setSpan(span, 0, txt.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                binding.log.append(spanString)
            } else if (bold) {
                val spanString = SpannableString(txt)
                val span = StyleSpan(Typeface.BOLD)
                spanString.setSpan(
                    span, 0, txt.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                binding.log.append(spanString)
            } else {
                binding.log.append(txt)
            }
        }
    }

    override fun dexDecodingStarted() {

    }

    override fun dexDecodingFinished(result: Boolean, strError: String?, strWarning: String?) {
        mDexDecodedCallback?.callbackFunc()
    }
}