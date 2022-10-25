package com.mcal.downloader

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.utils.isNetworkAvailable
import com.mcal.downloader.databinding.DownloaderActivityBinding

class DownloaderActivity : CustomizedLangActivity() {
    private var _binding: DownloaderActivityBinding? = null
    private val binding get() = _binding!!

    val tools = listOf(
        "android-framework.jar" to "$DOMAIN_COM/apkeditor/framework/$SDK/android.jar",
        "aapt" to "$DOMAIN_COM/apkeditor/bin/$ABI/aapt",
        "aapt2" to "$DOMAIN_COM/apkeditor/bin/$ABI/aapt2",
        "mycp" to "$DOMAIN_COM/apkeditor/bin/$ABI/mycp",
        "zipalign" to "$DOMAIN_COM/apkeditor/bin/$ABI/zipalign",
        //"aaptz" to "$DOMAIN_COM/apkeditor/bin/aaptz",
        "testkey.pk8" to "$DOMAIN_COM/apkeditor/keys/testkey.pk8",
        "testkey.x509.pem" to "$DOMAIN_COM/apkeditor/keys/testkey.x509.pem",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DownloaderActivityBinding.inflate(layoutInflater)
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

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    companion object {
        private const val DOMAIN_RU = "https://timscriptov.ru"
        private const val DOMAIN_COM = "https://timscriptov.com"

        private val ABI = Build.SUPPORTED_64_BIT_ABIS[0] ?: Build.SUPPORTED_32_BIT_ABIS[0] ?: "armeabi-v7a"
        private const val SDK = 33
    }
}
