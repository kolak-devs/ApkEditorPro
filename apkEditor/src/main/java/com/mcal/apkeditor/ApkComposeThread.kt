package com.mcal.apkeditor

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.mcal.apkeditor.ce.IApkMaking
import com.mcal.apkeditor.pro.DexEncoder
import com.mcal.apkeditor.smali.ISmaliAssembleCallback
import com.mcal.apkeditor.utils.AssetsInstaller
import com.mcal.apksigner.ApkSigner
import com.mcal.common.data.Preferences
import com.mcal.common.fastzip.FastZip
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.ScopedStorage.getApkEditorDir
import com.mcal.common.utils.createNewFile
import com.mcal.common.utilsOld.CommandRunner
import com.mcal.common.utilsOld.ITaskCallback
import com.mcal.common.utilsOld.ITaskCallback.TaskStepInfo
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.CoroutineContext

/**
 * @param ctx             Context
 * @param decodedFilePath path store all the decoded files
 * @param srcApkPath      where the source file is from
 * @param apkPath         target apk path
 */
class ApkComposeThread(
    private val ctx: Context,
    decodedFilePath: String?,
    srcApkPath: String?,
    apkPath: String?
) : ComposeThread(), ISmaliAssembleCallback {
    private val binRootPath: String
    private val aaptPath: String
    private val aaptPath2: String
    private val androidJarPath: String
    private var decodedFilePath: String? = null
    private var srcApkPath: String? = null
    private var targetApkPath: String? = null // Target APK path


    // Record all the dex file replaces
    private val dexReplaces: MutableMap<String, String> = HashMap()
    private val stepInfo: TaskStepInfo

    // Last time updating the smali assemble info
    private val lastUpdateAssembleTime: Long = 0

    // merge result
    private var tempApkPath: String? = null // if res modified, store the intermediate

    // Indicate succeed or not
    private var succeed = false
    var errMessage: String? = null
        private set

    // String resource modified or not
    private var stringModified = false

    // Manifest modified or not
    private var manifestModified = false

    // resource file added/deleted
    private var resFileModified = false

    // Samli file modified or not
    private var modifiedSmaliFolders: List<String>? = null

    // Add/Delete/Modify files
    private var addedFiles: MutableMap<String, String>? = null
    private var replacedFiles: MutableMap<String, String>? = null
    private var deletedFiles: MutableSet<String>? = null
    private var fileEntry2ZipEntry: Map<String, String>? = null
    private var taskCallback: ITaskCallback? = null

    // Flag to control run or not
    private var stopFlag = false
    private var bSignApk = false
    private var extraMaker: IApkMaking? = null

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
        stringModified = strModified
        this.manifestModified = manifestModified
        this.resFileModified = resFileModified
        this.modifiedSmaliFolders = modifiedSmaliFolders
        this.addedFiles = addedFiles
        this.deletedFiles = deletedFiles
        this.replacedFiles = replacedFiles
        this.fileEntry2ZipEntry = fileEntry2ZipEntry
        this.bSignApk = bSignApk
    }

    private var runningJob = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + runningJob

    override fun execute() = launch {
        doInBackground()
    }

    private suspend fun doInBackground(): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val rebuildResNeeded = isResourceModified
        // Sign & Cleanup
        if (BuildConfig.WITH_SIGN) {
            stepInfo.stepTotal = if (bSignApk) 2 else 1
        } else {
            stepInfo.stepTotal = 1
        }
        // Need to compile the resource
        if (rebuildResNeeded) stepInfo.stepTotal += 2
        // For DEX assembling
        modifiedSmaliFolders?.let { folders ->
            if (folders.isNotEmpty()) {
                stepInfo.stepTotal += folders.size
            }
        }
        do {
            // XML/String/Manifest modified, requires re-compiling
            if (rebuildResNeeded) {
                if (stopFlag) {
                    errMessage = "User request to stop"
                    break
                }
                setNextStep(ctx.getString(R.string.compose))
                if (!prepare()) {
                    break
                }

                // Compose resource and extract files
                if (stopFlag) {
                    errMessage = "User request to stop"
                    break
                }
                if (!composeResource()) {
                    break
                }
            }
            // Assemble DEX files
            modifiedSmaliFolders?.let { folders ->
                if (folders.isNotEmpty()) {
                    var assembleError = false
                    for (smaliFolder in folders) {
                        val smaliPath = "$decodedFilePath/$smaliFolder"
                        val dexName = getDexNameBySmaliFolder(smaliFolder)
                        setNextStep(ctx.getString(R.string.assemble_dex_file) + ": " + dexName)
                        try {
                            // Log.d("DEBUG", "Assemble " + smaliPath + " to " + dexName);
                            assembleSmali(smaliPath, dexName)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            errMessage = e.message
                            assembleError = true
                            break
                        }
                        if (stopFlag) {
                            errMessage = "User request to stop"
                            assembleError = true
                            break
                        }
                    }
                    if (assembleError) return@let
                }
            }
            if (rebuildResNeeded) {
                if (stopFlag) {
                    errMessage = "User request to stop"
                    break
                }
                setNextStep(ctx.getString(R.string.merge))
                try {
                    mergeApk()
                    removeResChanges()
                } catch (e: Exception) {
                    e.printStackTrace()
                    errMessage = ctx.getString(R.string.merge) + ": " + e.message
                    break
                }
            } else {
                translate2OriginEntry()
            }

            // Sign or not
            if (BuildConfig.WITH_SIGN && bSignApk) {
                if (stopFlag) {
                    errMessage = "User request to stop"
                    break
                }
                setNextStep(ctx.getString(R.string.sign))
                if (!signApk()) {
                    break
                }
            }

            // Clean up
            if (stopFlag) {
                errMessage = "User request to stop"
                break
            }
            setNextStep(ctx.getString(R.string.cleanup))
            cleanup()

            // For free version, make sure it longer enough, so that ad could be loaded
            if (!BuildConfig.IS_PRO) {
                val curTime = System.currentTimeMillis()
                if (curTime - startTime < 7500) {
                    try {
                        delay(7500 - (curTime - startTime))
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
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

    // When add/delete/modify some general images, rebuild is not need
    // But in sign step, we need to do the modification
    // Here we must translate it back to the original entry name
    private fun translate2OriginEntry() {
        // Do not need to translate
        if (fileEntry2ZipEntry.isNullOrEmpty()) {
            return
        }

        // Translate add records
        addedFiles?.let { files ->
            if (files.isNotEmpty()) {
                val newAdded: MutableMap<String, String> = HashMap()
                for ((key, value) in files) {
                    fileEntry2ZipEntry?.get(key).let { newKey ->
                        if (newKey != null) {
                            newAdded[newKey] = value
                        } else {
                            newAdded[key] = value
                        }
                    }
                }
                addedFiles = newAdded
            }
        }

        // Translate replace records
        replacedFiles?.let { files ->
            if (files.isNotEmpty()) {
                val newReplace: MutableMap<String, String> = HashMap()
                for ((key, value) in files) {
                    fileEntry2ZipEntry?.get(key).let { newKey ->
                        if (newKey != null) {
                            newReplace[newKey] = value
                        } else {
                            newReplace[key] = value
                        }
                    }
                }
                replacedFiles = newReplace
            }
        }

        // Translate delete records
        deletedFiles?.let { files ->
            if (files.isNotEmpty()) {
                val newDelete: MutableSet<String> = mutableSetOf()
                for (entry in files) {
                    fileEntry2ZipEntry?.get(entry).let { newEntry ->
                        if (newEntry != null) {
                            newDelete.add(newEntry)
                        } else {
                            newDelete.add(entry)
                        }
                    }
                }
                deletedFiles = newDelete
            }
        }
    }

    // Delete resource changes, as the resource change is already applied
    private fun removeResChanges() {
        addedFiles?.let {
            removeResInMap(it)
        }
        replacedFiles?.let {
            removeResInMap(it)
        }
        deletedFiles?.removeIf { s -> s.startsWith("res/") }
    }

    private fun removeResInMap(data: MutableMap<String, String>) {
        data.entries.removeIf { entry -> entry.key.startsWith("res/") }
    }

    private fun getDexNameBySmaliFolder(smaliFolder: String): String {
        if ("smali" == smaliFolder) {
            return "classes.dex"
        }
        return if (smaliFolder.startsWith("smali_")) {
            smaliFolder.substring("smali_".length) + ".dex"
        } else "$smaliFolder.dex"
    }

    private fun setNextStep(description: String) {
        stepInfo.stepIndex += 1
        stepInfo.stepDescription = description
        taskCallback?.setTaskStepInfo(stepInfo)
    }

    // Assemble smali to DEX
    @Throws(Throwable::class)
    private fun assembleSmali(smaliFilePath: String, dexFileName: String) {
        targetApkPath?.let { apkPath ->
            val dexFilePath = getPathInSameDirectory(
                apkPath,
                dexFileName
            )

            // Invoke DexEncoder.smali2Dex
            try {
                DexEncoder.smali2Dex(smaliFilePath, dexFilePath, this)

                // Record the replace
                dexReplaces[dexFileName] = dexFilePath
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
        }
    }

    private fun getPathInSameDirectory(path: String, name: String): String {
        val position = path.lastIndexOf('/')
        return path.substring(0, position + 1) + name
    }

    private fun cleanup() {
        // Delete the intermediate file
        if (tempApkPath != srcApkPath) {
            tempApkPath?.let { path ->
                if (File(path).exists()) {
                    val f = File(path)
                    f.delete()
                }
            }
        }

        // Delete all the decoded files
        // Do not delete the res directory any more, as the project must keep it
        val cr = CommandRunner()
        try {
            val tmpDir = ScopedStorage.getTmpDir()
            cr.runCommand("rm -rf $tmpDir", null, 10000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mergeApk() {
        // Before merge, call the extra maker
        val extraReplaces: MutableMap<String, String> = HashMap()
        if (extraMaker != null) {
            try {
                // Note: currently not support description update
                extraMaker?.prepareReplaces(ctx, tempApkPath, extraReplaces) { }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        var mappingLen = 0
        val sb1 = StringBuilder()
        fileEntry2ZipEntry?.let { entry ->
            for ((key, value) in entry) {
                sb1.append(key)
                sb1.append('\n')
                sb1.append(value)
                sb1.append('\n')
                mappingLen += key.toByteArray().size + value.toByteArray().size + 2
            }
        }
        var replaceLen = 0
        val sb2 = StringBuilder()
        addedFiles?.let { files ->
            for ((key, value) in files) {
                // For xml files, not replace it, as already compiled it into AXML
                if (key.startsWith("res/") && key.endsWith(".xml")) {
                    continue
                }
                sb2.append(key)
                sb2.append('\n')
                sb2.append(value)
                sb2.append('\n')
                replaceLen += key.toByteArray().size + value.toByteArray().size + 2
            }
        }
        replacedFiles?.let { files ->
            for ((key, value) in files) {
                if (key.startsWith("res/")) {
                    // For xml files, not replace it, as already compiled it into AXML
                    if (key.endsWith(".xml")) {
                        continue
                    }
                    // For 9.png files, not replace it, as already it is already compiled
                    if (key.endsWith(".9.png")) {
                        continue
                    }
                }
                sb2.append(key)
                sb2.append('\n')
                sb2.append(value)
                sb2.append('\n')
                replaceLen += key.toByteArray().size + value.toByteArray().size + 2
            }
        }
        for ((key, value) in extraReplaces) {
            // The extra one must be replaced
//            String key = entry.getKey();
//            if (key.startsWith("res/") && key.endsWith(".xml")) {
//                continue;
//            }
            sb2.append(key)
            sb2.append('\n')
            sb2.append(value)
            sb2.append('\n')
            replaceLen += key.toByteArray().size + value.toByteArray().size + 2
        }

        try {
            srcApkPath?.let { apkPath ->
                FastZip.repack(apkPath, getApkEditorDir().toString() + File.separator + "gen_unsigned.apk", replacedFiles, addedFiles, deletedFiles)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val isResourceModified: Boolean
        get() = (stringModified || manifestModified || resFileModified)

    private fun signApk(): Boolean {
        // When smali code is edited, also replace classes.dex
        replacedFiles?.putAll(dexReplaces)
        try {
            ApkSigner().signApk(
                getApkEditorDir().toString() + File.separator + "gen_unsigned.apk",
                getApkEditorDir().toString() + File.separator + "gen_signed.apk"
            )
            return true
        } catch (e: Exception) {
            val strHeader = ctx.resources
                .getString(R.string.sign_error)
            errMessage = strHeader + e.message
        }
        return false
    }

    private fun composeResource(): Boolean {
        tempApkPath = "$targetApkPath.in"
        return try {
            if (Preferences.isAapt2(ctx)) aapt2() else aapt()
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun aapt(): Boolean {
        val noVersionVectorOption = Preferences.getNoVersionVectorOption(aaptPath)
        val paramList: MutableList<String> = ArrayList()
        paramList.add(aaptPath)
        paramList.add("package")
        paramList.add("-f")
        paramList.add("-I")
        paramList.add(androidJarPath)
        paramList.add("-S")
        paramList.add("$decodedFilePath/res")
        paramList.add("-M")
        paramList.add("$decodedFilePath/AndroidManifest.xml")
        paramList.add("-F")
        tempApkPath?.let { apkPath ->
            paramList.add(apkPath)
        }
        if (noVersionVectorOption) {
            paramList.add("--no-version-vectors")
        }
        val startTime = System.currentTimeMillis()
        val cr = CommandRunner()
        val ret = cr.runCommand(paramList.toTypedArray(), null, null, 300 * 1000, true)
        Log.e("DEBUG", "aapt Time: " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds")
        if (!ret) {
            errMessage = cr.stdError
            return false
        }
        return true
    }

    @Throws(IOException::class)
    fun aapt2(): Boolean {
        val noVersionVectorOption = Preferences.getNoVersionVectorOption(aaptPath2)
        val args = ArrayList<String>()
        //compile resources
        args.add(aaptPath2)
        args.add("compile")
        args.add("--dir")
        args.add("$decodedFilePath/res")
        args.add("-o")
        val resPath = File(decodedFilePath, "build")
        resPath.mkdir()
        val outputPath = createNewFile(resPath, "resources.zip")
        args.add(outputPath.absolutePath)
        val startTime = System.currentTimeMillis()
        val cr = CommandRunner()
        val ret = cr.runCommand(args.toTypedArray(), null, null, 300 * 1000, true)
        if (!ret) {
            errMessage = cr.stdError
            return false
        }
        args.clear()

        //link resources
        args.add(aaptPath2)
        args.add("link")
        args.add("--allow-reserved-package-id")
        if (noVersionVectorOption) {
            args.add("--no-version-vectors")
        }
        args.add("--no-version-transitions")
        args.add("--auto-add-overlay")
        args.add("-I")
        args.add(androidJarPath)

        //add compiled resources
        val resources = resPath.listFiles()
        if (resources != null) {
            for (file in resources) {
                if (file.isDirectory || file.name == "resources.zip") {
                    continue
                }
                args.add("-R")
                args.add(file.absolutePath)
            }
        }
        val projectZip = File(resPath, "resources.zip")
        if (projectZip.exists()) {
            args.add("-R")
            args.add(projectZip.absolutePath)
        }
        args.add("--manifest")
        args.add("$decodedFilePath/AndroidManifest.xml")
        args.add("-o")
        tempApkPath?.let { apkpath ->
            args.add(apkpath)
        }
        val cr2 = CommandRunner()
        val ret2 = cr2.runCommand(
            args.toTypedArray(),
            null, null, 300 * 1000, true
        )
        Log.e("DEBUG", "aapt Time: " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds")
        if (!ret2) {
            errMessage = cr.stdError
            return false
        }
        return true
    }

    fun prepare(): Boolean {
        return try {
            prepare(ctx)
        } catch (e: Exception) {
            errMessage = e.message
            false
        }
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

    companion object {
        // This method will extract the necessary files
        @JvmStatic
        @Throws(Exception::class)
        fun prepare(ctx: Context): Boolean {
            var curVersion: String? = null
            try {
                val pInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                curVersion = pInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }

            // Prepare file
            val sp = ctx.getSharedPreferences("info", 0)
            val inited = sp.getBoolean("initialized", false)
            val lastVersion = sp.getString("version", "")
            return if (!inited || lastVersion != curVersion) {
                if (copyFiles(ctx)) {
                    val editor = sp.edit()
                    editor.putBoolean("initialized", true)
                    editor.putString("version", curVersion)
                    editor.apply()
                    return true
                }
                false
            } else {
                true
            }
        }

        // Copy aapt & android-framework.jar
        @Throws(Exception::class)
        private fun copyFiles(ctx: Context): Boolean {
            return try {
                try {
                    AssetsInstaller(ctx).install()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception("Can not copy file: " + e.message)
            }
        }
    }

    init {
        val fileDir = ctx.filesDir
        val rootDirectory = fileDir.absolutePath
        binRootPath = "$rootDirectory/bin"
        aaptPath = "$binRootPath/aapt"
        aaptPath2 = "$binRootPath/aapt2"
        androidJarPath = "$binRootPath/android-framework.jar"
        if (decodedFilePath != null) {
            this.decodedFilePath = decodedFilePath
        }
        if (srcApkPath != null) {
            this.srcApkPath = srcApkPath
        }
        if (apkPath != null) {
            targetApkPath = apkPath
        }
        if (srcApkPath != null) {
            tempApkPath = srcApkPath
        }
        stepInfo = TaskStepInfo()
    }
}