package com.mcal.apkeditor

import android.content.Context
import brut.androlib.Androlib
import com.mcal.androlib.options.BuildOptions
import com.mcal.androlib.utils.Logger
import com.mcal.apkeditor.utils.AssetsInstaller
import com.mcal.apksigner.ApkSigner
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.data.ReactivePreferences.getKeyAlias
import com.mcal.common.data.ReactivePreferences.getKeyPassword
import com.mcal.common.data.ReactivePreferences.getSigningPassword
import com.mcal.common.utils.ITaskCallback
import com.mcal.common.utils.ITaskCallback.TaskStepInfo
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.ScopedStorage.getKey
import com.mcal.common.utils.cleanup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.logging.Level
import kotlin.coroutines.CoroutineContext

/**
 * @param context             Context
 * @param decodedFilePath path store all the decoded files
 * @param apkPath         target apk path
 */
class ApkComposeThreadNew(
    private val context: Context,
    decodedFilePath: String,
    apkPath: String
) : ComposeThread(), Logger {
    private val mDecodedFilePath: String
    private val mTargetApkPath: String // Target APK path

    private val mStepInfo: TaskStepInfo

    // Indicate succeed or not
    private var isSucceed = false
    private var mErrorMessage: String? = null
    private var mTaskCallback: ITaskCallback? = null

    // Flag to control run or not
    private var isStopFlag = false
    private var isNeedSignApk = false

    private var runningJob = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + runningJob

    // resReplaces contains non-xml replaces
    // resFileModified means res added/deleted, or xml changed
    override fun setModification(
        strModified: Boolean,
        manifestModified: Boolean,
        resFileModified: Boolean,
        modifiedSmaliFolders: List<String>?,
        addedFiles: MutableMap<String, String>?,
        replacedFiles: MutableMap<String, String>?,
        deletedFiles: MutableSet<String>?,
        fileEntry2ZipEntry: Map<String, String>?,
        bSignApk: Boolean
    ) {
        this.isNeedSignApk = bSignApk
    }

    /**
     * Запуск фонового процесса
     */
    override fun execute() = launch {
        doInBackground()
    }

    /**
     * Компиляция в фоновом процессе
     */
    private suspend fun doInBackground(): Boolean = withContext(Dispatchers.IO) {
        // Make sure build directory is created
        val buildDir = File("$mDecodedFilePath/build")
        if (!buildDir.exists()) {
            buildDir.mkdir()
        }
        do {
            val unsignedApk = File(ScopedStorage.getApkEditorDir(), "app_unsigned.apk")
            val binDir = ScopedStorage.getBinDir()
            try {
                launch(Dispatchers.IO) {
                    unsignedApk.createNewFile()
                }
                mStepInfo.stepTotal = 12
                setNextStep(context.getString(R.string.build_preparing))
                AssetsInstaller(context).install()
                setNextStep(context.getString(R.string.build_compiling))

                val aaptFile = ScopedStorage.getAapt()
                val aapt2File = ScopedStorage.getAapt2()
                if (!aaptFile.canExecute()) {
                    mErrorMessage = "aapt is missing or not executable. Download it via Tools Manager."
                    break
                }
                if (ReactivePreferences.isAapt2() && !aapt2File.canExecute()) {
                    mErrorMessage = "aapt2 is missing or not executable. Download it via Tools Manager."
                    break
                }

                Androlib(BuildOptions().apply {
                    useNewBuildRules = ReactivePreferences.isAaptRules()
                    useJsonConfig = ReactivePreferences.isJsonConfig()
                    useAapt2 = ReactivePreferences.isAapt2()
                    aaptPath = aaptFile.path
                    aapt2Path = aapt2File.path
                    frameworkFolderLocation = binDir.path
                    ignoreMultiRes = ReactivePreferences.ignoreMultiResAsync()
                }, this@ApkComposeThreadNew).build(File(mDecodedFilePath), unsignedApk)
                setNextStep(context.getString(R.string.build_signing))
                if (!signApk(unsignedApk.path)) {
                    setNextStep(context.getString(R.string.message_signing_disabled))
                } else {
                    unsignedApk.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mErrorMessage = e.message
                break
            }
            // Clean up
            if (isStopFlag) {
                mErrorMessage = context.getString(R.string.build_canceled)
                break
            }
            setNextStep(context.getString(R.string.cleanup))
            ScopedStorage.getTmpDir().cleanup()
            ScopedStorage.getDecodedDir().cleanup()
            isSucceed = true
        } while (false)
        if (!isStopFlag) {
            if (isSucceed) {
                mTaskCallback?.taskSucceed()
            } else {
                mTaskCallback?.taskFailed(mErrorMessage)
            }
        }
        return@withContext true
    }

    private fun setNextStep(description: String) {
        mStepInfo.stepIndex += 1
        mStepInfo.stepDescription = description
        mTaskCallback?.setTaskStepInfo(mStepInfo)
    }

    private suspend fun signApk(inApk: String): Boolean {
        return if (ReactivePreferences.isSigningEnabled()) {
            if (ReactivePreferences.isCustomSigningEnabled()) {
                try {
                    getKey()?.let { keyFile ->
                        return ApkSigner().sign(File(inApk), File(mTargetApkPath), keyFile, getSigningPassword(), getKeyAlias(), getKeyPassword())
                    }
                } catch (e: ArrayIndexOutOfBoundsException) {
                    e.printStackTrace()
                }
            }
            val binDir = ScopedStorage.getBinDir()
            val pk8 = File(binDir, "testkey.pk8")
            val x509 = File(binDir, "testkey.x509.pem")
            try {
                if (!pk8.exists() || pk8.length() == 0L) {
                    context.assets.open("testkey.pk8").use { input ->
                        pk8.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                if (!x509.exists() || x509.length() == 0L) {
                    context.assets.open("testkey.x509.pem").use { input ->
                        x509.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return if (pk8.exists() && x509.exists()) {
                ApkSigner().sign(inApk, mTargetApkPath, pk8.absolutePath, x509.absolutePath)
            } else {
                false
            }
        } else false
    }

    override fun setTaskCallback(callback: ITaskCallback?) {
        this.mTaskCallback = callback
    }

    override fun stopRunning() {
        isStopFlag = true
        runningJob.cancel()
    }

    init {
        mDecodedFilePath = decodedFilePath
        mTargetApkPath = apkPath
        mStepInfo = TaskStepInfo()
    }

    override fun error(args: String?) {
        setNextStep(String.format("E: %s", args))
    }

    override fun log(level: Level, format: String?, ex: Throwable?) {
        val ch = level.name[0]
        val fmt = "%c: %s"
        format?.let {
            setNextStep(String.format(fmt, ch, format))
        }
        log(fmt, ch, ex)
    }

    private fun log(fmt: String, ch: Char, ex: Throwable?) {
        if (ex == null) {
            return
        }
        setNextStep(String.format(fmt, ch, ex.message))
        for (ste in ex.stackTrace) {
            setNextStep(String.format(fmt, ch, ste))
        }
        log(fmt, ch, ex.cause)
    }

    override fun fine(args: String?) {
        setNextStep(String.format("F: %s", args))
    }

    override fun warning(args: String?) {
        setNextStep(String.format("W: %s", args))
    }

    override fun info(args: String?) {
        setNextStep(String.format("I: %s", args))
    }
}