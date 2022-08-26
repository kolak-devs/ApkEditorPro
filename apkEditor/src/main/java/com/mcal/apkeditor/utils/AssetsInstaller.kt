package com.mcal.apkeditor.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.mcal.common.data.Preferences
import com.mcal.common.utils.copyFile
import java.io.File
import java.io.IOException

class AssetsInstaller(private val context: Context) {
    @Throws(Exception::class)
    fun install() {
        val path = File(context.filesDir.toString() + "/bin")
        if (!path.exists()) {
            path.mkdir()
        }
        prepare(path)
    }

    // This method will extract the necessary files
    @Throws(Exception::class)
    fun prepare(path: File) {
        var curVersion: String? = null
        try {
            val pInfo = context.packageManager
                .getPackageInfo(context.packageName, 0)
            curVersion = pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        // Prepare file
        val inited = Preferences.getInitialized()
        val lastVersion = Preferences.getVersionString()
        if (!inited || lastVersion != curVersion) {
            context.copyFile("key/testkey.pk8", File(path, "testkey.pk8"))
            context.copyFile("key/testkey.x509.pem", File(path, "testkey.x509.pem"))
            context.copyFile(Build.CPU_ABI + "/aapt", File(path, "aapt"))
            context.copyFile(Build.CPU_ABI + "/aapt2", File(path, "aapt2"))
            context.copyFile(Build.CPU_ABI + "/aaptz", File(path, "aaptz"))
            context.copyFile(Build.CPU_ABI + "/zipalign", File(path, "zipalign"))
            context.copyFile("android-framework.jar", File(path, "android-framework.jar"))
            context.copyFile("android-framework.jar", File(path, "1.apk"))
            context.copyFile("android-framework.jar", File(path, "1.apk"))
            val bin = File(context.filesDir, "mycp")
            if (!bin.exists()) {
                context.copyFile(Build.CPU_ABI + "/mycp", bin)
                bin.setExecutable(true)
            }
            createWorkFiles()
            Preferences.setInitialized(true)
            Preferences.setVersionString(curVersion)
        }
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