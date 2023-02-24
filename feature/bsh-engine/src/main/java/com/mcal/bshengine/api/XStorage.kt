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

    fun getStorageDir(): File {
        return Environment.getExternalStorageDirectory()
    }

    fun getApkFile(): File? {
        return apkFilePath?.let { File(it) }
    }

    fun getProjectDir(): File {
        return File(decodedDirPath)
    }

    fun getSmaliDir(): File {
        return File(getProjectDir(), "smali")
    }

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

    fun getResDir(): File {
        return File(getProjectDir(), "res")
    }

    fun getManifestFile(): File {
        return File(getProjectDir(), "AndroidManifest.xml")
    }
}