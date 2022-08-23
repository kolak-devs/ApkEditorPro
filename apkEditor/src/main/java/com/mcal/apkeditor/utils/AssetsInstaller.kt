package com.mcal.apkeditor.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.os.Build
import com.mcal.common.data.Preferences
import com.mcal.common.utilsOld.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class AssetsInstaller(private val context: Context) {

    @Throws(Exception::class)
    fun install() {
        val path = File(context.filesDir.toString() + "/bin")
        if (!path.exists()) {
            path.mkdir()
        }
        val assets = context.assets
        prepare(assets, path)
    }

    // This method will extract the necessary files
    @Throws(Exception::class)
    fun prepare(assets: AssetManager, path: File) {
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
            copyKeyPk8(assets, path)
            copyKeyPem(assets, path)
            copyAapt(assets, path)
            copyAapt2(assets, path)
            copyAaptZ(assets, path)
            copyZipAlign(assets, path)
            copyFramework(assets, path)
            copyFrameworkApk(assets, path)
            copyMycp(assets)
            createWorkFiles()
            Preferences.setInitialized(true)
            Preferences.setVersionString(curVersion)
        }
    }

    private fun createWorkFiles() {
        // If need to limit the new version, does not need to create such files
        var f = File(context.filesDir, "work.xml")
        if (!f.exists()) {
            try {
                f.createNewFile()
                f.setWritable(true)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        f = File(context.filesDir, "work.db")
        if (!f.exists()) {
            try {
                f.createNewFile()
                f.setWritable(true)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    private fun copyMycp(assets: AssetManager) {
        // Copy mycp
        try {
            val bin = File(context.filesDir, "mycp")
            if (!bin.exists()) {
                val input = assets.open(Build.CPU_ABI + "/mycp")
                val output = FileOutputStream(bin)
                IOUtils.copy(input, output)
                input.close()
                output.close()
                bin.setExecutable(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun copyKeyPk8(assets: AssetManager, outDir: File) {
        val pk8 = File(outDir, "testkey.pk8")
        val input = assets.open("key/testkey.pk8")
        val output = FileOutputStream(pk8)
        IOUtils.copy(input, output)
        input.close()
        output.close()
    }

    @Throws(IOException::class)
    private fun copyKeyPem(assets: AssetManager, outDir: File) {
        val pk8 = File(outDir, "testkey.x509.pem")
        val input = assets.open("key/testkey.x509.pem")
        val output: OutputStream = FileOutputStream(pk8)
        IOUtils.copy(input, output)
        input.close()
        output.close()
    }

    @Throws(IOException::class)
    private fun copyAaptZ(assets: AssetManager, outDir: File) {
        val aapt2 = File(outDir, "aaptz")
        val input = assets.open("aaptz")
        val output = FileOutputStream(aapt2)
        IOUtils.copy(input, output)
        input.close()
        output.close()
        aapt2.setExecutable(true)
    }

    @Throws(IOException::class)
    private fun copyAapt(assets: AssetManager, outDir: File) {
        val aapt = File(outDir, "aapt")
        val input = assets.open(Build.CPU_ABI + "/aapt")
        val output = FileOutputStream(aapt)
        IOUtils.copy(input, output)
        input.close()
        output.close()
        aapt.setExecutable(true)
    }

    @Throws(IOException::class)
    private fun copyAapt2(assets: AssetManager, outDir: File) {
        val aapt2 = File(outDir, "aapt2")
        val input = assets.open(Build.CPU_ABI + "/aapt2")
        val output = FileOutputStream(aapt2)
        IOUtils.copy(input, output)
        input.close()
        output.close()
        aapt2.setExecutable(true)
    }

    @Throws(IOException::class)
    private fun copyZipAlign(assets: AssetManager, outDir: File) {
        val aapt2 = File(outDir, "zipalign")
        val input = assets.open(Build.CPU_ABI + "/zipalign")
        val output = FileOutputStream(aapt2)
        IOUtils.copy(input, output)
        input.close()
        output.close()
        aapt2.setExecutable(true)
    }

    // Copy android-framework.jar
    @Throws(IOException::class)
    private fun copyFramework(assets: AssetManager, outDir: File) {
        val aapt2 = File(outDir, "android-framework.jar")
        val input = assets.open("android-framework.jar")
        val output = FileOutputStream(aapt2)
        IOUtils.copy(input, output)
        input.close()
        output.close()
        aapt2.setExecutable(true)
    }

    @Throws(IOException::class)
    private fun copyFrameworkApk(assets: AssetManager, outDir: File) {
        val aapt2 = File(outDir, "1.apk")
        val input = assets.open("android-framework.jar")
        val output = FileOutputStream(aapt2)
        IOUtils.copy(input, output)
        input.close()
        output.close()
        aapt2.setExecutable(true)
    }
}