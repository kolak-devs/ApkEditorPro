package com.mcal.common.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.utils.ScopedStorage.getMyCp
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.Contract
import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile


fun readFile(fileName: String) = readFile(File(fileName))

fun readFile(fileName: File) = fileName.inputStream().readBytes().toString(Charsets.UTF_8)

// Support root mode and non-root mode
@Contract("_ -> new")
private fun createCommandRunner(isRootMode: Boolean): CommandInterface {
    return if (isRootMode) {
        RootCommand()
    } else {
        CommandRunner()
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

@Throws(IOException::class)
fun deleteFile(file: File): Boolean {
    if (file.isDirectory) {
        file.listFiles()?.let { files ->
            for (f in files) {
                deleteFile(f)
            }
        }
    }
    return Files.deleteIfExists(file.toPath())
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

@Throws(IOException::class)
fun writeToFile(targetFile: String?, content: String) {
    var fos: FileOutputStream? = null
    try {
        fos = FileOutputStream(targetFile)
        fos.write(content.toByteArray())
    } finally {
        closeQuietly(fos)
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
fun toByteArray(input: InputStream): ByteArray {
    val output = ByteArrayOutputStream()
    copyFile(input, output)
    return output.toByteArray()
}

/**
 * @param path временный файл
 * @param realPath реальный путь к файлу
 */
@Throws(java.lang.Exception::class)
fun copyBack(path: String, realPath: String, isRootMode: Boolean) {
    val rc = createCommandRunner(isRootMode)
    var strCmd = "cp"
    val bin = getMyCp()
    if (bin.exists()) {
        strCmd = bin.path
    }
    val copyRet = rc.runCommand(String.format("$strCmd %s \"%s\"", path, realPath), null, 3000)

    // Copy file failed, use the original file
    if (!copyRet) {
        throw java.lang.Exception("Can not write file to $realPath")
    }
}

@Throws(IOException::class)
fun Context.copyAssetsFile(filename: String, output: File) {
    copyFile(assets.open(filename), FileOutputStream(output))
}

@Throws(IOException::class)
fun copyFile(srcFilePath: String, dstFilePath: String) {
    copyFile(File(srcFilePath), File(dstFilePath))
}

@Throws(IOException::class)
fun copyFile(filename: File, output: File) {
    copyFile(FileInputStream(filename), FileOutputStream(output))
}

@Throws(IOException::class)
fun copyFile(input: InputStream, output: File) {
    copyFile(input, FileOutputStream(output))
}

@Throws(IOException::class)
fun copyFile(source: InputStream, target: OutputStream) {
    val buf = ByteArray(8192)
    var length: Int
    while (source.read(buf).also { length = it } != -1) {
        target.write(buf, 0, length)
    }
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
    val folder = File(ScopedStorage.getApkEditorDir().path + File.separator + path)
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

@Throws(IOException::class)
fun readFully(input: InputStream, buf: ByteArray) {
    var read = 0
    while (read < buf.size) {
        val ret = input.read(buf, read, buf.size - read)
        read += if (ret != -1) {
            ret
        } else {
            break
        }
    }
}

fun InputStream.readText(charset: Charset = Charsets.UTF_8): String {
    return this.bufferedReader(charset).use { it.readText() }
}

fun getDecodeDirectory(): String? {
    var str: String?
    runBlocking {
        str = ReactivePreferences.getDecodeDirectory()
    }
    if (str != null) {
        if (str!!.endsWith("/")) {
            str = str!!.substring(0, str!!.length - 1)
        }
        if (dirCanWrite(str!!)) {
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

fun writeObjectToFile(filePath: String, obj: Any): Boolean {
    val file = File(filePath)
    var objOut: ObjectOutputStream? = null
    try {
        objOut = ObjectOutputStream(FileOutputStream(file))
        objOut.writeObject(obj)
        objOut.flush()
        return true
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        if (objOut != null) {
            try {
                objOut.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    return false
}

fun readObjectFromFile(filePath: String?): Any? {
    if (filePath == null) {
        return null
    }
    var result: Any? = null
    val file = File(filePath)
    var objIn: ObjectInputStream? = null
    try {
        objIn = ObjectInputStream(FileInputStream(file))
        result = objIn.readObject()
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    } finally {
        if (objIn != null) {
            try {
                objIn.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    return result
}

fun closeQuietly(c: Closeable?) {
    if (c != null) {
        try {
            c.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

fun closeQuietly(zfile: ZipFile?) {
    if (zfile != null) {
        try {
            zfile.close()
        } catch (ignored: IOException) {
        }
    }
}

fun InputStream.readInputStream(): String {
    val sb = StringBuilder()
    try {
        val br = BufferedReader(InputStreamReader(this))
        var line: String?
        while (br.readLine().also { line = it } != null) {
            sb.append(line)
            sb.append('\n')
        }
        br.close()
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
    return sb.toString().trim()
}

fun Context.getFileName(uri: Uri): String? = when (uri.scheme) {
    ContentResolver.SCHEME_CONTENT -> getContentFileName(uri)
    else -> uri.path?.let(::File)?.name
}

private fun Context.getContentFileName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        cursor.moveToFirst()
        return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
    }
}.getOrNull()

@Throws(IOException::class)
fun addFile(targetPath: String, filePath: InputStream) {
    val pos = targetPath.lastIndexOf(47.toChar())
    val dirPath = File(targetPath.substring(0, pos))
    if (!dirPath.exists()) {
        dirPath.mkdirs()
    }
    copyFile(filePath, FileOutputStream(targetPath))
}

@Throws(java.lang.Exception::class)
fun makeDir(dirPath: String?, folderName: String) {
    val f = File(dirPath, folderName)
    if (f.exists()) {
        throw java.lang.Exception(java.lang.String.valueOf(Log.e("IOUtils", String.format("Folder %s exits", folderName))))
    }
    f.mkdir()
}