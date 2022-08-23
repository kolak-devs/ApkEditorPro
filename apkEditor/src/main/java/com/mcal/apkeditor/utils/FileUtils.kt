package com.mcal.apkeditor.utils

import android.content.Context
import android.os.Environment
import com.mcal.common.utils.ScopedStorage
import java.io.File
import java.io.IOException

object FileUtils {
    @JvmStatic
    @Throws(IOException::class)
    fun createNewFile(parent: File, name: String): File {
        val createdFile = File(parent, name)
        parent.mkdirs()
        createdFile.createNewFile()
        return createdFile
    }

    @JvmStatic
    @Throws(Exception::class)
    fun makeBackupDir(ctx: Context): String {
        return makeDir(ctx, "backup")
    }

    @JvmStatic
    @Throws(Exception::class)
    fun makeDir(ctx: Context, dirName: String): String {
        if (!exist()) {
            throw Exception("Can not find sd card.")
        }
        var subDir = ""
        val packagePath = ctx.packageName
        if (packagePath.startsWith("com.mcal.apkeditor.apkpermremover")) {
            subDir = "/.ApkPermRemover/$dirName/"
        } else if (packagePath.startsWith("com.mcal.apkeditor.pmaster")) {
            subDir = "/PermMaster/$dirName/"
        } else if (packagePath == "com.mcal.apkeditor.permissionmanager") {
            subDir = "/PermMaster/$dirName/"
        } else if (packagePath.startsWith("com.mcal.apkeditor")) {
            subDir = "/ApkEditor/$dirName/"
        } else if (packagePath.startsWith("com.mcal.apkeditor.appdm")) {
            subDir = "/HackAppData/$dirName/"
        } else if (packagePath.startsWith("com.mcal.apkeditor.pro")) {
            subDir = "/ApkEditor/$dirName/"
        } else if (packagePath.startsWith("com.mcal.apkeditor.legacy")) {
            subDir = "/ApkEditor/$dirName/"
        }
        val rootDir = ScopedStorage.storageDirectory.path
        val targetDir = rootDir + subDir
        val f = File(targetDir)
        if (!f.exists()) {
            f.mkdirs()
        }
        return targetDir
    }

    fun exist(): Boolean = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}