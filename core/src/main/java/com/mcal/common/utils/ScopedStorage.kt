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
        val path = File(filesDir, "decoded")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getApkEditorDir(): File {
        val parent = File(storageDirectory, Environment.DIRECTORY_DOCUMENTS)
        val path = File(parent, "ApkEditor")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getBinDir(): File {
        val path = File(filesDir, "bin")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getTmpDir(): File {
        val path = File(getApkEditorDir(), "tmp")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getTempDir(): File {
        val path = File(getApkEditorDir(), "temp")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getBackupsDir(): File {
        val path = File(getApkEditorDir(), "backups")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getDataDir(): File {
        val path = File(filesDir, "data")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getProjects(): File {
        val path = File(getApkEditorDir(), "projects")
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

    @JvmStatic
    fun getKeysDir(): File {
        val path = File(getApkEditorDir(), "keys")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getKey(): File? {
        val ks = getKeysDir().listFiles()
        return ks?.get(0)
    }
}