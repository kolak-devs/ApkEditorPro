package com.mcal.apkeditor.utils

import android.content.Context
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.copyAssetsFile
import java.io.File
import java.io.IOException

class AssetsInstaller(private val context: Context) {
    @Throws(Exception::class)
    fun install() {
        val path = ScopedStorage.getBinDir()
        if (!path.exists()) {
            path.mkdir()
        }
        context.copyAssetsFile("key/testkey.pk8", File(path, "testkey.pk8"))
        context.copyAssetsFile("key/testkey.x509.pem", File(path, "testkey.x509.pem"))
        createWorkFiles()
    }

    private fun createWorkFiles() {
        // If need to limit the new version, does not need to create such files
        var file = File(context.filesDir, "work.xml")
        if (!file.exists()) {
            try {
                file.createNewFile()
                file.setWritable(true)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        file = File(context.filesDir, "work.db")
        if (!file.exists()) {
            try {
                file.createNewFile()
                file.setWritable(true)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}