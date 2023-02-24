package com.mcal.bshengine.api

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

object XFileHelper {
    @JvmStatic
    fun getFiles(path: String): List<File> {
        return getFiles(File(path))
    }

    @JvmStatic
    fun getFiles(file: File): List<File> {
        val files = arrayListOf<File>()
        file.walk().forEach {
            files.add(it)
        }
        return files
    }

    @JvmStatic
    fun readFileAsLines(fileName: File): List<String> = fileName.bufferedReader().readLines()

    @JvmStatic
    fun readFile(path: String) = FileInputStream(File(path)).readBytes().toString(StandardCharsets.UTF_8)

    @JvmStatic
    fun readFile(file: File) =
        FileInputStream(file).readBytes().toString(StandardCharsets.UTF_8)

    @JvmStatic
    fun readFile(inputStream: InputStream) =
        inputStream.readBytes().toString(StandardCharsets.UTF_8)

    @JvmStatic
    fun writeText(path: String, fileContent: String) {
        File(path).writeText(fileContent)
    }

    @JvmStatic
    fun writeText(file: File, fileContent: String) {
        file.writeText(fileContent)
    }
}