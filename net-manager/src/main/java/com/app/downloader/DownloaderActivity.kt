package com.app.downloader

import alirezat775.lib.downloader.Downloader
import alirezat775.lib.downloader.core.OnDownloadListener
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.utils.ScopedStorage
import com.mcal.downloader.R
import kotlinx.android.synthetic.main.activity_net_manager.*
import java.io.File

class DownloaderActivity : CustomizedLangActivity() {

    private val TAG: String = this::class.java.name

    private val handler: Handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_net_manager)
        initFullScreen()
        start_download_btn.setOnClickListener {
            execute()
        }
    }

    private fun execute() {
        val percentFramework: TextView = findViewById(R.id.percent_framework)
        val sizeFramework: TextView = findViewById(R.id.size_framework)
        val totalSizeFramework: TextView = findViewById(R.id.total_size_framework)
        val progressBarFramework: ProgressBar = findViewById(R.id.download_progress_framework)
        getFramework(
            "https://www.timscriptov.ru/apkeditor/framework/32/android.jar",
            ScopedStorage.binDir.path + File.separator + "android-framework.jar",
            percentFramework,
            sizeFramework,
            totalSizeFramework,
            progressBarFramework
        )
        val percentAapt: TextView = findViewById(R.id.percent_aapt)
        val sizeAapt: TextView = findViewById(R.id.size_aapt)
        val totalSizeAapt: TextView = findViewById(R.id.total_size_aapt)
        val progressBarAapt: ProgressBar = findViewById(R.id.download_progress_aapt)
        getFramework(
            "https://www.timscriptov.ru/apkeditor/bin/arm64-v8a/aapt",
            ScopedStorage.binDir.path + File.separator + "aapt",
            percentAapt,
            sizeAapt,
            totalSizeAapt,
            progressBarAapt
        )

        val percentAapt2: TextView = findViewById(R.id.percent_aapt2)
        val sizeAapt2: TextView = findViewById(R.id.size_aapt2)
        val totalSizeAapt2: TextView = findViewById(R.id.total_size_aapt2)
        val progressBarAapt2: ProgressBar = findViewById(R.id.download_progress_aapt2)
        getFramework(
            "https://www.timscriptov.ru/apkeditor/bin/arm64-v8a/aapt2",
            ScopedStorage.binDir.path + File.separator + "aapt2",
            percentAapt2,
            sizeAapt2,
            totalSizeAapt2,
            progressBarAapt2
        )

        val percentMyCp: TextView = findViewById(R.id.percent_mycp)
        val sizeMyCp: TextView = findViewById(R.id.size_mycp)
        val totalSizeMyCp: TextView = findViewById(R.id.total_size_mycp)
        val progressBarMyCp: ProgressBar = findViewById(R.id.download_progress_mycp)
        getFramework(
            "https://www.timscriptov.ru/apkeditor/bin/arm64-v8a/mycp",
            ScopedStorage.filesDir.path + File.separator + "mycp",
            percentMyCp,
            sizeMyCp,
            totalSizeMyCp,
            progressBarMyCp
        )

        val percentZipAlign: TextView = findViewById(R.id.percent_zipalign)
        val sizeZipAlign: TextView = findViewById(R.id.size_zipalign)
        val totalSizeZipAlign: TextView = findViewById(R.id.total_size_zipalign)
        val progressBarZipAlign: ProgressBar = findViewById(R.id.download_progress_zipalign)
        getFramework(
            "https://www.timscriptov.ru/apkeditor/bin/arm64-v8a/zipalign",
            ScopedStorage.binDir.path + File.separator + "zipalign",
            percentZipAlign,
            sizeZipAlign,
            totalSizeZipAlign,
            progressBarZipAlign
        )
    }

    private fun getFramework(
        url: String,
        path: String,
        percentText: TextView,
        sizeText: TextView,
        totalSizeText: TextView,
        progressBar: ProgressBar
    ) {
        Downloader.Builder(
            this@DownloaderActivity,
            url
        ).downloadListener(object : OnDownloadListener {
            override fun onStart() {
                handler.post { showResult("Start") }
                Log.d(TAG, "onStart")
            }

            override fun onPause() {
                handler.post { showResult("Pause") }
                Log.d(TAG, "onPause")
            }

            override fun onResume() {
                handler.post { showResult("Resume") }
                Log.d(TAG, "onResume")
            }

            override fun onProgressUpdate(percent: Int, downloadedSize: Int, totalSize: Int) {
                handler.post {
                    showResult("Progress Update")
                    if (totalSize != -1) {
                        percentText.text = percent.toString().plus("%")
                        sizeText.text = getSize(downloadedSize)
                        totalSizeText.text = getSize(totalSize)
                        progressBar.progress = percent
                    } else {
                        percentText.text = ""
                        sizeText.text = getSize(downloadedSize)
                        totalSizeText.text = ""
                        progressBar.isIndeterminate = true
                    }

                }
                Log.d(
                    TAG,
                    "onProgressUpdate: percent --> $percent downloadedSize --> $downloadedSize totalSize --> $totalSize "
                )
            }

            override fun onCompleted(file: File?) {
//                file?.let {
//                    CoroutineScope(Dispatchers.IO).launch {
//                        Paths.get(file.path)
//                            .move(Paths.get(path))
//                    }
//                }
                handler.post {
                    progressBar.isIndeterminate = false
                    progressBar.progress = 100
                    showResult("Completed $file")
                }
            }

            override fun onFailure(reason: String?) {
                handler.post { showResult("Failure: reason --> $reason") }
                Log.d(TAG, "onFailure: reason --> $reason")
            }

            override fun onCancel() {
                handler.post { showResult("Cancel") }
                Log.d(TAG, "onCancel")
            }
        }).build().download()
    }

    fun showResult(msg: String) {
        current_status_txt.text = msg
    }

    fun getSize(size: Int): String {
        var s = ""
        val kb = (size / 1024).toDouble()
        val mb = kb / 1024
        val gb = kb / 1024
        val tb = kb / 1024
        if (size < 1024) {
            s = "$size Bytes"
        } else if (size >= 1024 && size < 1024 * 1024) {
            s = String.format("%.2f", kb) + " KB"
        } else if (size >= 1024 * 1024 && size < 1024 * 1024 * 1024) {
            s = String.format("%.2f", mb) + " MB"
        } else if (size >= 1024 * 1024 * 1024 && size < 1024 * 1024 * 1024 * 1024) {
            s = String.format("%.2f", gb) + " GB"
        } else if (size >= 1024 * 1024 * 1024 * 1024) {
            s = String.format("%.2f", tb) + " TB"
        }
        return s
    }
}
