package com.mcal.bshengine.api

import java.io.*
import java.nio.charset.StandardCharsets

class XFileHelper {
    fun getFiles(path: String): List<File> {
        return getFiles(File(path))
    }

    fun getFiles(file: File): List<File> {
        val files = arrayListOf<File>()
        file.walk().forEach {
            files.add(it)
        }
        return files
    }

    fun readFileAsLines(fileName: File): List<String> = fileName.bufferedReader().readLines()

    fun readFile(path: String): String = FileInputStream(File(path)).readBytes().toString(StandardCharsets.UTF_8)

    fun readFile(file: File): String =
        FileInputStream(file).readBytes().toString(StandardCharsets.UTF_8)

    fun readFile(inputStream: InputStream): String =
        inputStream.readBytes().toString(StandardCharsets.UTF_8)

    fun writeText(path: String, fileContent: String) {
        File(path).writeText(fileContent)
    }

    fun writeText(file: File, fileContent: String) {
        file.writeText(fileContent)
    }

    fun isFile(file: File): Boolean {
        return file.isFile
    }

    fun isDirectory(file: File): Boolean {
        return file.isDirectory
    }

    fun isSmali(file: File): Boolean {
        return file.name.endsWith(".smali")
    }

    fun isXml(file: File): Boolean {
        return file.name.endsWith(".xml")
    }

    @Throws(IOException::class)
    fun copyFile(path: String, destinationPath: String) {
        copyFile(File(path), File(destinationPath))
    }

    @Throws(IOException::class)
    fun copyFile(file: File, destinationFile: File) {
        copyFile(FileInputStream(file), FileOutputStream(destinationFile))
    }

    @Throws(IOException::class)
    fun copyFile(input: InputStream, destinationFile: File) {
        copyFile(input, FileOutputStream(destinationFile))
    }

    @Throws(IOException::class)
    fun copyFile(source: InputStream, target: OutputStream) {
        val buf = ByteArray(8192)
        var length: Int
        while (source.read(buf).also { length = it } != -1) {
            target.write(buf, 0, length)
        }
    }
}