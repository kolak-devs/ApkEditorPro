package com.mcal.common.utils

import android.os.Environment
import com.mcal.common.App
import java.io.File

object ScopedStorage {
    @JvmStatic
    val filesDir: File
        get() = App.getContext().filesDir

    @JvmStatic
    val cacheDir: File
        get() = App.getContext().cacheDir

    @JvmStatic
    val storageDirectory: File
        get() = Environment.getExternalStorageDirectory()

    @JvmStatic
    fun getDecodedDir(): File {
        val path = File(filesDir.path + File.separator + "decoded")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getApkEditorDir(): File {
        val path = File("$storageDirectory/ApkEditor")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getBinDir(): File {
        val path = File(filesDir.path + File.separator + "bin")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getTmpDir(): File {
        val path = File(getApkEditorDir().path + File.separator + "tmp")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getTempDir(): File {
        val path = File(getApkEditorDir().path + File.separator + "temp")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getBackupsDir(): File {
        val path = File(getApkEditorDir().path + File.separator + "backups")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getDataDir(): File {
        val path = File(filesDir.path + File.separator + "data")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getPublicKey(): File {
        return File(getBinDir(), "testkey.x509.pem")
    }

    @JvmStatic
    fun getPrivateKey(): File {
        return File(getBinDir(), "testkey.pk8")
    }

    @JvmStatic
    fun getAapt(): File {
        val path = File(getBinDir(), "aapt")
        path.setExecutable(true)
        return path
    }

    @JvmStatic
    fun getAapt2(): File {
        val path = File(getBinDir(), "aapt2")
        path.setExecutable(true)
        return path
    }
}