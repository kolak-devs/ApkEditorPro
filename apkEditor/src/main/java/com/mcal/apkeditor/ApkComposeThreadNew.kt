package com.mcal.apkeditor

import android.content.Context
import android.util.Log
import brut.androlib.Androlib
import brut.androlib.options.BuildOptions
import com.mcal.androlib.util.Logger
import com.mcal.apkeditor.ce.IApkMaking
import com.mcal.apkeditor.smali.ISmaliAssembleCallback
import com.mcal.apkeditor.utils.AssetsInstaller
import com.mcal.apksigner.ApkSigner
import com.mcal.common.data.Preferences
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.cleanup
import com.mcal.common.utilsOld.ITaskCallback
import com.mcal.common.utilsOld.ITaskCallback.TaskStepInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.logging.Level
import kotlin.coroutines.CoroutineContext


/**
 * @param ctx             Context
 * @param decodedFilePath path store all the decoded files
 * @param apkPath         target apk path
 */
class ApkComposeThreadNew(
    private val ctx: Context,
    decodedFilePath: String,
    apkPath: String
) : ComposeThread(), ISmaliAssembleCallback, Logger {
    private val decodedFilePath: String
    private val targetApkPath: String // Target APK path

    private val stepInfo: TaskStepInfo

    // Last time updating the smali assemble info
    private val lastUpdateAssembleTime: Long = 0

    // Indicate succeed or not
    private var succeed = false
    var errMessage: String? = null
        private set
    private var taskCallback: ITaskCallback? = null

    // Flag to control run or not
    private var stopFlag = false
    private var bSignApk = false
    private var extraMaker: IApkMaking? = null

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
        this.bSignApk = bSignApk
    }

    override fun execute() = launch {
        doInBackground()
    }

    private suspend fun doInBackground(): Boolean = withContext(Dispatchers.IO) {
        // Make sure build directory is created
        val buildDir = File("$decodedFilePath/build")
        if (!buildDir.exists()) {
            if (buildDir.mkdir()) {
                Log.e(javaClass.name, "$buildDir created")
            }
        }
        do {
            val binFolder = File(ctx.filesDir.toString() + "/bin")
            val options = BuildOptions()
            options.useAapt2 = Preferences.isAapt2(ctx)
            options.aaptPath = binFolder.toString() + File.separator + if (Preferences.isAapt2(ctx)) "aapt2" else "aapt"
            options.frameworkFolderLocation = binFolder.path
            val androlib = Androlib(options, this@ApkComposeThreadNew)
            try {
                val tmp = File(ctx.cacheDir, "app.apk")
                launch(Dispatchers.IO) {
                    tmp.createNewFile()
                }
                stepInfo.stepTotal = 4
                try {
                    setNextStep("Preparing...")
                    AssetsInstaller(ctx).install()
                } catch (e: Exception) {
                    errMessage = e.message
                }
                setNextStep("Compiling...")
                androlib.buildOptions.noCrunch = true
                androlib.build(File(decodedFilePath), tmp)
                setNextStep("Signing...")
                if (!signApk(tmp.path)) {
                    break
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errMessage = e.message
                break
            }
            // Clean up
            if (stopFlag) {
                errMessage = "User request to stop"
                break
            }
            setNextStep(ctx.getString(R.string.cleanup))
            File(ScopedStorage.storageDirectory.path + "/ApkEditor/tmp").cleanup()
            succeed = true
        } while (false)
        if (!stopFlag) {
            if (succeed) {
                taskCallback?.taskSucceed()
            } else {
                taskCallback?.taskFailed(errMessage)
            }
        }
        return@withContext true
    }

    private fun setNextStep(description: String) {
        stepInfo.stepIndex += 1
        stepInfo.stepDescription = description
        taskCallback?.setTaskStepInfo(stepInfo)
    }

    private fun signApk(inApk: String): Boolean {
        try {
            ApkSigner().signApk(inApk, targetApkPath)
            return true
        } catch (e: Exception) {
            val strHeader = ctx.resources.getString(R.string.sign_error)
            errMessage = strHeader + e.message
        }
        return false
    }

    override fun setTaskCallback(callback: ITaskCallback?) {
        this.taskCallback = callback
    }

    override fun updateAssembledFiles(assembledFiles: Int, totalFiles: Int) {
        val curTime = System.currentTimeMillis()
        if (curTime > lastUpdateAssembleTime + 500) {
            val fmt = ctx.getString(R.string.assemble_dex_detail)
            stepInfo.stepDescription = String.format(fmt, assembledFiles, totalFiles)
            taskCallback?.setTaskStepInfo(stepInfo)
        }
    }

    override fun stopRunning() {
        stopFlag = true
        runningJob.cancel()
    }

    override fun setExtraMaker(extraMaker: IApkMaking?) {
        this.extraMaker = extraMaker
    }

    init {
        this.decodedFilePath = decodedFilePath
        targetApkPath = apkPath
        stepInfo = TaskStepInfo()
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
//        setNextStep(String.format("F: %s", args))
    }

    override fun warning(args: String?) {
        setNextStep(String.format("W: %s", args))
    }

    override fun info(args: String?) {
        setNextStep(String.format("I: %s", args))
    }
}