package com.mcal.bshengine.api

import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

class XFileHelper() {
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