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

    fun getApk(): File? {
        return apkFilePath?.let { File(it) }
    }

    fun getProject(): File {
        return File(decodedDirPath)
    }

    fun getSmali(): File {
        return File(getProject(), "smali")
    }

    fun getRes(): File {
        return File(getProject(), "res")
    }

    fun getManifest(): File {
        return File(getProject(), "AndroidManifest.xml")
    }
}