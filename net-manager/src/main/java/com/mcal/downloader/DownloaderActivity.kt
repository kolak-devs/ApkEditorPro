package com.mcal.downloader

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.utils.isNetworkAvailable
import com.mcal.downloader.databinding.DownloaderActivityBinding

class DownloaderActivity : CustomizedLangActivity() {
    lateinit var binding: DownloaderActivityBinding
    val tools = listOf(
        "android-framework.jar" to "https://timscriptov.ru/apkeditor/framework/$SDK/android.jar",
        "aapt" to "https://timscriptov.ru/apkeditor/bin/$ABI/aapt",
        "aapt2" to "https://timscriptov.ru/apkeditor/bin/$ABI/aapt2",
        "mycp" to "https://timscriptov.ru/apkeditor/bin/$ABI/mycp",
        "zipalign" to "https://timscriptov.ru/apkeditor/bin/$ABI/zipalign",
        //"aaptz" to "https://timscriptov.ru/apkeditor/bin/aaptz",
        "testkey.pk8" to "https://timscriptov.ru/apkeditor/keys/testkey.pk8",
        "testkey.x509.pem" to "https://timscriptov.ru/apkeditor/keys/testkey.x509.pem",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DownloaderActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(R.id.toolbar, "Управление инструментарием", true)
        val adapter = DownloaderAdapter(tools)
        val recyclerView = binding.recyclerview
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        binding.errors.visibility = if (!isNetworkAvailable(this)) {
            binding.errors.text = "Отсутствует Интернет подключение"
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    companion object {
        private val ABI = Build.SUPPORTED_64_BIT_ABIS[0] ?: Build.SUPPORTED_32_BIT_ABIS[0] ?: "armeabi-v7a"
        private const val SDK = 33
    }
}
