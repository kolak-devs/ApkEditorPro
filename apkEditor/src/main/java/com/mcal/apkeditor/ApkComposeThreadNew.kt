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
import com.mcal.common.utils.ScopedStorage.filesDir
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
            val tmpApkFile = File(ScopedStorage.getTmpDir(), "app.apk")
            val binDir = ScopedStorage.getBinDir()
            try {
                launch(Dispatchers.IO) {
                    tmpApkFile.createNewFile()
                }
                mStepInfo.stepTotal = 12
                setNextStep(context.getString(R.string.build_preparing))
                AssetsInstaller(context).install()
                setNextStep(context.getString(R.string.build_compiling))


                val binDirPath = binDir.path
                Androlib(BuildOptions().apply {
                    isAaptRules = ReactivePreferences.isAaptRules()
                    isJsonConfig = ReactivePreferences.isJsonConfig()
                    useAapt2 = ReactivePreferences.isAapt2()
                    aaptPath = binDirPath + File.separator + "aapt"
                    aapt2Path = binDirPath + File.separator + "aapt2"
                    frameworkFolderLocation = binDirPath
                }, this@ApkComposeThreadNew).build(File(mDecodedFilePath), tmpApkFile)
                setNextStep(context.getString(R.string.build_signing))
                if (!signApk(tmpApkFile.path)) {
                    setNextStep(context.getString(R.string.message_signing_disabled))
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
            tmpApkFile.delete()
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
            if (!ReactivePreferences.isCustomSigningEnabled()) {
                getKey()?.let { keyFile ->
                    return ApkSigner().sign(File(inApk), File(mTargetApkPath), keyFile, getSigningPassword(), getKeyAlias(), getKeyPassword())
                } ?: run {
                    return ApkSigner().sign(
                        inApk,
                        mTargetApkPath,
                        filesDir.toString() + File.separator + "bin/testkey.pk8",
                        filesDir.toString() + File.separator + "bin/testkey.x509.pem"
                    )
                }
            } else {
                return ApkSigner().sign(
                    inApk,
                    mTargetApkPath,
                    filesDir.toString() + File.separator + "bin/testkey.pk8",
                    filesDir.toString() + File.separator + "bin/testkey.x509.pem"
                )
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