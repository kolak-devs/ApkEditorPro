package com.mcal.downloader

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.data.Constants.getDomain
import com.mcal.common.utils.isNetworkAvailable
import com.mcal.downloader.databinding.DownloaderActivityBinding

class DownloaderActivity : CustomizedLangActivity() {
    private lateinit var binding: DownloaderActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DownloaderActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(id = R.id.toolbar, title = getString(R.string.tools_manager), back = true)
        binding.recyclerview.apply {
            layoutManager = LinearLayoutManager(this@DownloaderActivity)
            val domain = getDomain()
            adapter = DownloaderAdapter(
                mutableListOf(
                    "android-framework.jar" to "$domain/apkeditor/framework/$SDK/android.jar",
                    "aapt" to "$domain/apkeditor/bin/$ABI/aapt",
                    "aapt2" to "$domain/apkeditor/bin/$ABI/aapt2",
                    "mycp" to "$domain/apkeditor/bin/$ABI/mycp",
                    "zipalign" to "$domain/apkeditor/bin/$ABI/zipalign",
                    //"aaptz" to "DOMAIN/apkeditor/bin/aaptz",
                    "testkey.pk8" to "$domain/apkeditor/keys/testkey.pk8",
                    "testkey.x509.pem" to "$domain/apkeditor/keys/testkey.x509.pem",
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isNetworkAvailable(this)) {
            binding.errors.text = getString(R.string.network_available)
            setVisibility(binding.errors, View.VISIBLE)
        } else {
            setVisibility(binding.errors, View.GONE)
        }
    }

    companion object {
        private val ABI = Build.SUPPORTED_64_BIT_ABIS[0] ?: Build.SUPPORTED_32_BIT_ABIS[0] ?: "armeabi-v7a"
        private const val SDK = 33
    }
}
