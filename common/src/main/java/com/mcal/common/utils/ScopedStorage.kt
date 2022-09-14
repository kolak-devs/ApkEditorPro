package com.mcal.common.utils

import android.os.Environment
import com.mcal.common.App
import com.mcal.common.utilsOld.StorageUtils
import java.io.File

object ScopedStorage {
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
        val path = File(App.getContext().filesDir.path + File.separator + "bin")
        if (!path.exists()) path.mkdirs()
        return path
    }

    @JvmStatic
    fun getDataDir(): File {
        val path = File(App.getContext().filesDir.path + File.separator + "data")
        if (!path.exists()) path.mkdirs()
        return path
    }

    // Find the pattern
    @JvmStatic
    val externalStoragePath: String?
        get() {
            val internalPath = storageDirectory.path
            val files = App.getContext().getExternalFilesDirs(null)
            if (files != null) {
                // Find the pattern
                var appendedLen = 0
                for (f in files) {
                    val path = f.path
                    if (path.startsWith(internalPath)) {
                        appendedLen = path.length - internalPath.length
                        break
                    }
                }
                for (f in files) {
                    val path = f.path
                    if (!path.startsWith(internalPath)) {
                        return path.substring(0, path.length - appendedLen)
                    }
                }
            }
            var path: String? = null
            val storageList = StorageUtils.getStorageList()
            if (storageList != null) {
                for (si in storageList) {
                    if (!si.internal && !si.readonly) {
                        path = si.path
                        break
                    }
                }
            }
            return path
        }
}