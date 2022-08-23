package com.mcal.neweditor.editor2.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object FileUtils {
    fun readFileAsLinesUsingBufferedReader(fileName: String): List<String>
            = File(fileName).bufferedReader().readLines()

    fun readFileAsLinesUsingReadLines(fileName: String): List<String>
            = File(fileName).readLines()

    fun readFileAsTextUsingInputStream(fileName: String)
            = File(fileName).inputStream().readBytes().toString(Charsets.UTF_8)

    fun readFileDirectlyAsText(fileName: String): String
            = File(fileName).readText(Charsets.UTF_8)

    fun writeText(fileName: String, fileContent: String) {
        File(fileName).writeText(fileContent)
    }
}