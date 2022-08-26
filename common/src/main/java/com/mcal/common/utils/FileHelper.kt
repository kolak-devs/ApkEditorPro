package com.mcal.common.utils

import android.content.Context
import android.content.res.AssetManager
import android.os.Environment
import com.mcal.common.utilsOld.CommandRunner
import com.mcal.common.utilsOld.SDCard
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

fun Path.exists(): Boolean = Files.exists(this)

fun exist(): Boolean = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

fun Path.isFile(): Boolean = !Files.isDirectory(this)

fun Path.move(dest: Path, overwrite: Boolean = false): Boolean {
    return if (isFile()) {
        if (dest.exists()) {
            if (overwrite) {
                //Perform the move operation. REPLACE_EXISTING is needed for
                //replacing a file
                Files.move(this, dest, StandardCopyOption.REPLACE_EXISTING)
                true
            } else {
                false
            }
        } else {
            //Perform the move operation
            Files.move(this, dest)
            true
        }
    } else {
        false
    }
}

fun recursiveModifiedTime(files: Array<File>): Long {
    var modified: Long = 0
    for (i in files.indices) {
        val subModified = recursiveModifiedTime(files[i])
        if (subModified > modified) {
            modified = subModified
        }
    }
    return modified
}

fun recursiveModifiedTime(file: File): Long {
    var modified = file.lastModified()
    if (file.isDirectory) {
        file.listFiles()?.let { subFiles ->
            for (i in subFiles.indices) {
                val subModified = recursiveModifiedTime(subFiles[i])
                if (subModified > modified) {
                    modified = subModified
                }
            }
        }
    }
    return modified
}

@Throws(IOException::class)
fun deleteAll(f: File) {
    if (f.isDirectory) {
        f.listFiles()?.let { files ->
            for (c in files) {
                deleteAll(c)
            }
        }
    }
    f.delete()
}

@Throws(IOException::class)
fun copyFile(srcFile: File, dstFile: File) {
    srcFile.copyTo(dstFile)
}

@Throws(IOException::class)
fun copyFile(srcFilePath: String, dstFilePath: String) {
    copyFile(File(srcFilePath), File(dstFilePath))
}

fun writeToFile(fileName: String, lines: List<String>) {
    var bw: BufferedWriter? = null
    try {
        bw = BufferedWriter(FileWriter(fileName))
        for (line in lines) {
            bw.write(line)
            bw.write("\n")
        }
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        bw?.apply {
            try {
                bw.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}

fun writeToFile(fileName: String, data: ByteArray) {
    var fos: FileOutputStream? = null
    try {
        fos = FileOutputStream(fileName)
        fos.write(data)
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        fos?.apply {
            try {
                fos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}

private val notAllowedChars = charArrayOf('\"', '/', '\\', ':', '*', '?', '<', '>', '|')

private fun isNotAllowed(c: Char): Boolean {
    for (nc in notAllowedChars) {
        if (c == nc) {
            return true
        }
    }
    return false
}

fun reviseFileName(filename: String?): String {
    if (filename == null) {
        return ""
    }
    var modified = false
    val sb = StringBuilder()
    for (element in filename) {
        if (isNotAllowed(element)) {
            sb.append('_')
            modified = true
        } else {
            sb.append(element)
        }
    }
    return if (modified) {
        sb.toString()
    } else {
        filename
    }
}

fun Context.copyFile(filename: String, output: File) {
    this.assets.open(filename).use { stream ->
        output.outputStream().use {
            stream.copyTo(it)
        }
    }
}

@Throws(IOException::class)
fun createNewFile(parent: File, name: String): File {
    val createdFile = File(parent, name)
    parent.mkdirs()
    createdFile.createNewFile()
    return createdFile
}

@Throws(Exception::class)
fun makeBackupDir(ctx: Context): String {
    return makeDir(ctx, "backup")
}

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

fun File.cleanup() {
    val cr = CommandRunner()
    try {
        cr.runCommand("rm -rf $this", null, 10000)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}