package com.mcal.downloader

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mcal.common.activities.CustomizedLangActivity

class DownloaderActivity : CustomizedLangActivity() {
    val tools = listOf(
        "android-framework.jar" to "https://www.timscriptov.ru/apkeditor/framework/32/android.jar",
        "aapt" to "https://www.timscriptov.ru/apkeditor/bin/arm64-v8a/aapt",
        "aapt2" to "https://www.timscriptov.ru/apkeditor/bin/arm64-v8a/aapt2",
        "mycp" to "https://www.timscriptov.ru/apkeditor/bin/arm64-v8a/mycp",
        "zipalign" to "https://www.timscriptov.ru/apkeditor/bin/arm64-v8a/zipalign",
        "android-framework.jar" to "https://www.timscriptov.ru/apkeditor/framework/32/android.jar"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloader)
        setupToolbar(R.id.toolbar, "Управление инструментарием", true)

        val adapter = DownloaderAdapter(tools)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
}
