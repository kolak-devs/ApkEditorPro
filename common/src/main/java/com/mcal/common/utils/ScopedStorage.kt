package com.mcal.common.utils

import android.os.Environment
import com.mcal.common.App
import java.io.File

object ScopedStorage {
    @JvmStatic
    fun exist(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    @JvmStatic
    fun getSizeDescription(fileSize: Long): String {
        if (fileSize >= 1024 * 1024) {
            val mb = 1.0f * fileSize / 1024 / 1024
            return String.format("%.2f M", mb)
        } else if (fileSize >= 1024) {
            val kb = 1.0f * fileSize / 1024
            return String.format("%.2f K", kb)
        }
        return "$fileSize B"
    }

    @JvmStatic
    @Throws(Exception::class)
    fun makeDir(dirName: String): File {
        val folder = File(getApkEditorDir().path + dirName)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    @JvmStatic
    val filesDir: File
        get() = App.getContext().filesDir

    @JvmStatic
    val storageDirectory: File
        get() = Environment.getExternalStorageDirectory()

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
    fun getBackupDir(): File {
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
}