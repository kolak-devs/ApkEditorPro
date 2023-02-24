package com.mcal.bshengine.api

import android.os.Environment
import java.io.File

class XStorage(private var decodedDir: String, private var apkFile: String?) {

    fun getStorageDir(): File {
        return Environment.getExternalStorageDirectory()
    }

    fun getApk(): File? {
        return apkFile?.let { File(it) }
    }

    fun getProject(): File {
        return File(decodedDir)
    }

    fun getSmali(): File {
        return File(getProject().toString() + File.separator + "smali")
    }

    fun getRes(): File {
        return File(getProject().toString() + File.separator + "res")
    }

    fun getManifest(): File {
        return File(getProject().toString() + File.separator + "AndroidManifest.xml")
    }
}