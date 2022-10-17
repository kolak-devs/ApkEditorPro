package com.mcal.bshengine.api

import android.content.Context
import android.os.Environment
import java.io.File

class ZScopedStorage(context: Context, private var decodedDir: String) {
    fun getStorageDir(): File {
        return Environment.getExternalStorageDirectory()
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