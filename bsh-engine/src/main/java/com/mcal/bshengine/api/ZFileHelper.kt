package com.mcal.bshengine.api

import android.content.Context
import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

class ZFileHelper(context: Context) {
    @Throws(IOException::class)
    fun readFile(path: String): String {
        return String(Files.readAllBytes(Paths.get(path)), Charset.forName("UTF-8"))
    }

    @Throws(IOException::class)
    fun writeToFile(path: String, text: String) {
        val writer: Writer = BufferedWriter(OutputStreamWriter(FileOutputStream(File(path))))
        writer.write(text)
        writer.close()
    }
}