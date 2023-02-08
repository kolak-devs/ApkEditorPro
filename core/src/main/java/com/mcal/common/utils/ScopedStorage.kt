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
        return App.getContext().getExternalFilesDir(null) ?: getApkEditorDir()
    }

    @JvmStatic
    fun getPatchesDir(): File {
        val path = File(getApkEditorDir(), "patches")
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
    fun getFramework(): File {
        return File(getBinDir(), "android-framework.jar")
    }

    @JvmStatic
    fun getTmpDir(): File {
        val path = File(getApkEditorDir(), "tmp")
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
    fun getAndroidDebugKey(): File {
        return File(getBinDir(), "androiddebug.jks")
    }

    @JvmStatic
    fun getAapt(): File {
        val path = File(getBinDir(), "aapt")
        path.setExecutable(true)
        return path
    }

    @JvmStatic
    fun getMyCp(): File {
        val path = File(getBinDir(), "mycp")
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
        return getKeysDir().listFiles()?.get(0)
    }

    @JvmStatic
    fun isToolsInstalled(): Boolean {
        return getAapt().exists() or getAapt2().exists() and getMyCp().exists() and getAndroidDebugKey().exists() and
                (getFramework().exists() and getFramework().isFile)
    }
}