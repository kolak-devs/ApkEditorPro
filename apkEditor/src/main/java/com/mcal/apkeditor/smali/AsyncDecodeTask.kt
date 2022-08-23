package com.mcal.apkeditor.smali

import android.content.Context
import android.util.Log
import com.mcal.apkeditor.pro.DexDecoder
import com.mcal.common.utilsOld.IOUtils
import com.mcal.common.utilsOld.SDCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.coroutines.CoroutineContext

class AsyncDecodeTask(
    private val context: Context,
    private val mApkPath: String,
    private val mDecodeRootPath: String,
    private val mCallback: IDecodeTaskCallback?
) : CoroutineScope {
    // Record all dex file paths which are extracted from apk file
    private val dexFileList: MutableList<String> = ArrayList()
    private var strError: String? = null
    private var strWarning: String? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    fun execute() = launch {
        onPreExecute()
        val result = doInBackground()
        onPostExecute(result)
    }

    private fun onPreExecute() {
        mCallback?.dexDecodingStarted()
    }

    private suspend fun doInBackground(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            CoroutineScope(Dispatchers.IO).launch {

            }
            doAllJobs()
            true
        } catch (e: Exception) {
            strError = e.message
            false
        }
    }

    private fun onPostExecute(result: Boolean) {
        mCallback?.let { callback ->
            if (result) {
                File(mDecodeRootPath).listFiles()?.let { listFile ->
                    for (file in listFile) {
                        if (file.name.endsWith(".dex")) {
                            if (file.delete()) {
                                Log.e(javaClass.name, "$file deleted")
                            }
                        }
                    }
                }
                callback.dexDecodingFinished(true, null, strWarning)
            } else {
                callback.dexDecodingFinished(false, strError, null)
            }
        }
    }

    @Throws(Exception::class)
    private fun prepareDexFiles() {
        val tmpDirectory = SDCard.makeDir(context, "tmp")
        var zipFile: ZipFile? = null
        try {
            zipFile = ZipFile(mApkPath)
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                if (name.endsWith(".dex") && !name.contains("/")) {
                    unzipDex2File(zipFile, entry, tmpDirectory + name)
                }
            }
        } catch (e1: IOException) {
            e1.printStackTrace()
        } finally {
            zipFile?.let {
                try {
                    zipFile.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun prepareMainDex() {
        val tmpDirectory = SDCard.makeDir(context, "tmp")
        var zipFile: ZipFile? = null
        try {
            val name = "classes.dex"
            zipFile = ZipFile(mApkPath)
            val entry = zipFile.getEntry(name)
            unzipDex2File(zipFile, entry, tmpDirectory + name)
        } catch (e1: IOException) {
            e1.printStackTrace()
        } finally {
            zipFile?.let {
                try {
                    zipFile.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun unzipDex2File(zipFile: ZipFile, entry: ZipEntry, filePath: String) {
        var input: InputStream? = null
        var output: FileOutputStream? = null
        try {
            input = zipFile.getInputStream(entry)
            output = FileOutputStream(filePath)
            IOUtils.copy(input, output)
            dexFileList.add(filePath)
        } finally {
            input?.let {
                try {
                    input.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            output?.let {
                try {
                    output.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Throws(Exception::class)
    fun doAllJobs() {
        prepareDexFiles()
        decodeDexFiles()
        removeDexFiles()
    }

    // Only decode the classes.dex
    @Throws(Exception::class)
    fun decodeMainDex() {
        prepareMainDex()
        decodeDexFiles()
        removeDexFiles()
    }

    private fun removeDexFiles() {
        for (filePath in dexFileList) {
            val f = File(filePath)
            f.delete()
        }
    }

    private fun decodeDexFiles() {
        for (dexFilePath in dexFileList) {
            val decoder = DexDecoder(dexFilePath)
            var directory = "$mDecodeRootPath/smali"
            // Not the default dex file
            if (!dexFilePath.endsWith("/classes.dex")) {
                val position = dexFilePath.lastIndexOf("/")
                val dexName = dexFilePath.substring(
                    position + 1,
                    dexFilePath.length - 4
                )
                directory = "$mDecodeRootPath/smali_$dexName"
            }
            createDirectoryIfNotExist(directory)
            try {
                decoder.dex2smali(directory)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (strWarning == null) {
                strWarning = decoder.warning
            }
        }
    }

    private fun createDirectoryIfNotExist(directory: String) {
        val dir = File(directory)
        if (dir.exists()) {
            dir.mkdir()
        }
    }

    interface IDecodeTaskCallback {
        fun dexDecodingStarted()
        fun dexDecodingFinished(
            result: Boolean, strError: String?,
            strWarning: String?
        )
    }
}