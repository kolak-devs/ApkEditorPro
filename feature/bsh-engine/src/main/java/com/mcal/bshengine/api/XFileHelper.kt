package com.mcal.bshengine.api

import java.io.*
import java.nio.charset.StandardCharsets

class XFileHelper {
    /**
     * Since: 2.4.5
     */
    fun getFiles(path: String): List<File> {
        return getFiles(File(path))
    }

    /**
     * Since: 2.4.5
     */
    fun getFiles(file: File): List<File> {
        val files = arrayListOf<File>()
        file.walk().forEach {
            files.add(it)
        }
        return files
    }

    /**
     * Since: 2.4.6
     */
    fun getFilterFiles(file: File, skipFiles: List<String>): List<File> {
        val files = arrayListOf<File>()
        file.walk().filter { smaliFile ->
            var isInterestedFile = false
            val path = smaliFile.path
            skipFiles.forEach { whiteFileListName ->
                isInterestedFile = !whiteFileListName.contains(path)
            }
            isInterestedFile
        }.forEach {
            files.add(it)
        }
        return files
    }

    /**
     * Since: 2.4.5
     */
    fun readFileAsLines(fileName: File): List<String> = fileName.bufferedReader().readLines()

    /**
     * Since: 2.4.5
     */
    fun readFile(path: String): String = FileInputStream(File(path)).readBytes().toString(StandardCharsets.UTF_8)

    /**
     * Since: 2.4.5
     */
    fun readFile(file: File): String =
        FileInputStream(file).readBytes().toString(StandardCharsets.UTF_8)

    /**
     * Since: 2.4.5
     */
    fun readFile(inputStream: InputStream): String =
        inputStream.readBytes().toString(StandardCharsets.UTF_8)

    /**
     * Since: 2.4.5
     */
    fun writeText(path: String, fileContent: String) {
        File(path).writeText(fileContent)
    }

    /**
     * Since: 2.4.5
     */
    fun writeText(file: File, fileContent: String) {
        file.writeText(fileContent)
    }

    /**
     * Since: 2.4.5
     */
    fun isFile(file: File): Boolean {
        return file.isFile
    }

    /**
     * Since: 2.4.5
     */
    fun isDirectory(file: File): Boolean {
        return file.isDirectory
    }

    /**
     * Since: 2.4.5
     */
    fun isSmali(file: File): Boolean {
        return file.name.endsWith(".smali")
    }

    /**
     * Since: 2.4.6
     */
    fun isXml(file: File): Boolean {
        return file.name.endsWith(".xml")
    }

    /**
     * Since: 2.4.6
     */
    fun deleteRecursively(file: File) {
        file.deleteRecursively()
    }

    /**
     * Since: 2.4.6
     */
    @Throws(IOException::class)
    fun copyFile(path: String, destinationPath: String) {
        copyFile(File(path), File(destinationPath))
    }

    /**
     * Since: 2.4.6
     */
    @Throws(IOException::class)
    fun copyFile(file: File, destinationFile: File) {
        copyFile(FileInputStream(file), FileOutputStream(destinationFile))
    }

    /**
     * Since: 2.4.6
     */
    @Throws(IOException::class)
    fun copyFile(input: InputStream, destinationFile: File) {
        copyFile(input, FileOutputStream(destinationFile))
    }

    /**
     * Since: 2.4.6
     */
    @Throws(IOException::class)
    fun copyFile(source: InputStream, target: OutputStream) {
        val buf = ByteArray(8192)
        var length: Int
        while (source.read(buf).also { length = it } != -1) {
            target.write(buf, 0, length)
        }
    }
}