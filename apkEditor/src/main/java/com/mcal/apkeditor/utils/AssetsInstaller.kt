package com.mcal.apkeditor.utils

import android.content.Context
import java.io.File
import java.io.IOException

class AssetsInstaller(private val context: Context) {
    @Throws(Exception::class)
    fun install() {
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