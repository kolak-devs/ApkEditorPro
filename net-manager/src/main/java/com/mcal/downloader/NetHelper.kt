package com.mcal.downloader

import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.mcal.common.utils.copyFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetHelper {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    fun download(url: String, destFile: File, container: LinearLayout) {
        val linear = container.getChildAt(0) as ViewGroup
        val progressBar = linear.getChildAt(2) as LinearProgressIndicator
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    response.body?.let { body ->
                        val contentLength = body.contentLength().toInt()
                        val source = body.source()
                        val sink = destFile.sink().buffer()
                        val sinkBuffer = sink.buffer
                        var totalBytesRead: Long = 0
                        val bufferSize = 8 * 1024
                        var bytesRead: Long
                        while (source.read(sinkBuffer, bufferSize.toLong())
                                .also { bytesRead = it } != -1L
                        ) {
                            sink.emit()
                            totalBytesRead += bytesRead
                            val progress = (totalBytesRead * 100 / contentLength).toInt()
                            CoroutineScope(Dispatchers.Main).launch {
                                progressBar.visibility = View.VISIBLE
                                if (contentLength != -1) {
                                    progressBar.progress = progress
                                } else {
                                    progressBar.isIndeterminate = true
                                }
                            }
                        }
                        sink.flush()
                        sink.close()
                        source.close()
                        CoroutineScope(Dispatchers.Main).launch {
                            progressBar.visibility = View.INVISIBLE
                            val btn = container.getChildAt(1) as ImageButton
                            val context = btn.context
                            val icon = ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.round_file_download_done,
                                context.theme
                            )
                            btn.setImageDrawable(icon)
                        }
                        if (destFile.exists()) {
                            val path = destFile.path
                            if (path.endsWith(".jar")) {
                                copyFile(path, path.replace(destFile.name, "1.apk"))
                            }
                        }
                    }
                }
            }
        })
    }
}