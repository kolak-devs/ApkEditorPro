package com.mcal.downloader

import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException

class NetHelper {
    /**
     * @param url ссылка на файл для загрузки
     * @param destFile путь где сохранить файл
     * @param listener интерфейс для передачи прогресса
     */
    @Throws(IOException::class)
    private fun download(url: String, destFile: File, listener: NetHelperListener) {
        val client = OkHttpClient()
        val request: Request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        response.body?.let { body ->
            val contentLength = body.contentLength()
            val source = body.source()
            val sink = destFile.sink().buffer()
            val sinkBuffer = sink.buffer()
            var totalBytesRead: Long = 0
            val bufferSize = 8 * 1024
            var bytesRead: Long
            while (source.read(sinkBuffer, bufferSize.toLong()).also { bytesRead = it } != -1L) {
                sink.emit()
                totalBytesRead += bytesRead
                val progress = (totalBytesRead * 100 / contentLength).toInt()
                listener.progress(progress)
            }
            sink.flush()
            sink.close()
            source.close()
        }
    }

    fun getSize(size: Int): String {
        var s = ""
        val kb = (size / 1024).toDouble()
        val mb = kb / 1024
        val gb = kb / 1024
        val tb = kb / 1024
        if (size < 1024) {
            s = "$size Bytes"
        } else if (size in 1024..1_048_575) { //1024*1024
            s = String.format("%.2f", kb) + " KB"
        } else if (size in 1_048_576..1_073_741_823) { //1024 * 1024 * 1024
            s = String.format("%.2f", mb) + " MB"
        } else if (size in 1_073_741_824..1_099_511_627_775) { //1024 * 1024 * 1024 * 1024
            s = String.format("%.2f", gb) + " GB"
        } else if (size >= 1_099_511_627_776) { //1024 * 1024 * 1024 * 1024
            s = String.format("%.2f", tb) + " TB"
        }
        return s
    }
}