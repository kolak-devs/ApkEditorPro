package com.mcal.common.utils

import android.content.Context
import android.os.Environment
import com.mcal.common.data.Preferences
import com.mcal.common.utils.ScopedStorage.getBinDir
import com.mcal.common.utilsOld.CommandInterface
import com.mcal.common.utilsOld.CommandRunner
import org.jetbrains.annotations.Contract
import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

// Support root mode and non-root mode
@Contract("_ -> new")
private fun createCommandRunner(isRootMode: Boolean): CommandInterface {
    return if (isRootMode) {
        RootCommand()
    } else {
        CommandRunner()
    }
}

/**
 * @param path временный файл
 * @param realPath реальный путь к файлу
 */
@Throws(java.lang.Exception::class)
fun copyBack(path: String, realPath: String, isRootMode: Boolean) {
    val rc = createCommandRunner(isRootMode)
    var strCmd = "cp"
    val bin = File(getBinDir(), "mycp")
    if (bin.exists()) {
        strCmd = bin.path
    }
    val copyRet = rc.runCommand(String.format("$strCmd %s \"%s\"", path, realPath), null, 3000)

    // Copy file failed, use the original file
    if (!copyRet) {
        throw java.lang.Exception("Can not write file to $realPath")
    }
}

fun Path.exists(): Boolean = Files.exists(this)

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

@Throws(IOException::class)
fun Context.copyAssetsFile(filename: String, output: File) {
    copyFileStream(this.assets.open(filename), FileOutputStream(output))
}

@Throws(IOException::class)
fun copyFile(srcFilePath: String, dstFilePath: String) {
    copyFile(File(srcFilePath), File(dstFilePath))
}

@Throws(IOException::class)
fun copyFile(filename: File, output: File) {
    copyFileStream(FileInputStream(filename), FileOutputStream(output))
}

@Throws(IOException::class)
fun copyFileStream(input: InputStream, output: OutputStream) {
    val buffer = ByteArray(1024)
    var length: Int = input.read(buffer)
    while ((length) > 0) {
        output.write(buffer, 0, length)
        length = input.read(buffer)
    }
    input.close()
    output.flush()
    output.close()
}

@Throws(IOException::class)
fun createNewFile(parent: File, name: String): File {
    val createdFile = File(parent, name)
    parent.mkdirs()
    createdFile.createNewFile()
    return createdFile
}

fun exist(): Boolean = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

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

fun makeDir(path: String): File {
    val folder = File(ScopedStorage.getApkEditorDir().path + path)
    if (!folder.exists()) {
        folder.mkdirs()
    }
    return folder
}

fun File.cleanup() {
    val cr = CommandRunner()
    try {
        cr.runCommand("rm -rf $this", null, 10000)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun InputStream.readText(charset: Charset = Charsets.UTF_8): String {
    return this.bufferedReader(charset).use { it.readText() }
}

fun getDecodeDirectory(): String? {
    var str = Preferences.getDecodeDirectory()
    if (str != null) {
        if (str.endsWith("/")) {
            str = str.substring(0, str.length - 1)
        }
        if (dirCanWrite(str)) {
            return str
        }
    }
    return null
}

// Can write to the directory or not
private fun dirCanWrite(dir: String): Boolean {
    val f = File(dir)
    if (f.exists() && f.isDirectory) {
        val rand = getRandomString(8)
        val tryF = File(f, rand)
        val ret = tryF.mkdir()
        if (ret) {
            tryF.delete()
        }
        return ret
    }
    return false
}