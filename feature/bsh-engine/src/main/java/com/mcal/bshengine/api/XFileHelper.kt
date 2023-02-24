package com.mcal.bshengine.api

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
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
}