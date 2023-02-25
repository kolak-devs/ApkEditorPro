package com.mcal.bshengine.api

import android.os.Environment
import java.io.File

class XStorage() {
    private lateinit var decodedDirPath: String
    private var apkFilePath: String? = null

    fun setDecodedDir(dir: String) {
        decodedDirPath = dir
    }

    fun setApkPath(path: String?) {
        apkFilePath = path
    }

    /**
     * Since: 2.4.5
     */
    fun getStorageDir(): File {
        return Environment.getExternalStorageDirectory()
    }

    /**
     * Since: 2.4.5
     */
    fun getApkFile(): File? {
        return apkFilePath?.let { File(it) }
    }

    /**
     * Since: 2.4.5
     */
    fun getProjectDir(): File {
        return File(decodedDirPath)
    }

    /**
     * Since: 2.4.5
     */
    fun getSmaliDir(): File {
        return File(getProjectDir(), "smali")
    }

    /**
     * Since: 2.4.5
     */
    fun getAllSmaliDirs(): List<File> {
        val files = arrayListOf<File>()
        getProjectDir().walk().maxDepth(1).forEach { file ->
            if (file.isDirectory) {
                val name = file.name
                if (name == "smali" || name.startsWith("smali_classes")) {
                    files.add(file)
                }
            }
        }
        return files
    }

    /**
     * Since: 2.4.5
     */
    fun getResDir(): File {
        return File(getProjectDir(), "res")
    }

    /**
     * Since: 2.4.5
     */
    fun getManifestFile(): File {
        return File(getProjectDir(), "AndroidManifest.xml")
    }
}