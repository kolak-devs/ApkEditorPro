package com.mcal.downloader

import android.os.Build
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mcal.common.activities.CustomizedLangActivity

class DownloaderActivity : CustomizedLangActivity() {
    val tools = listOf(
        "android-framework.jar" to "https://timscriptov.ru/apkeditor/framework/$SDK/android.jar",
        "aapt" to "https://timscriptov.ru/apkeditor/bin/$ABI/aapt",
        "aapt2" to "https://timscriptov.ru/apkeditor/bin/$ABI/aapt2",
        "mycp" to "https://timscriptov.ru/apkeditor/bin/$ABI/mycp",
        "zipalign" to "https://timscriptov.ru/apkeditor/bin/$ABI/zipalign",
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

    companion object {
        private val ABI = Build.CPU_ABI ?: Build.CPU_ABI2 ?: "armeabi-v7a"
        private const val SDK = 33
    }
}
