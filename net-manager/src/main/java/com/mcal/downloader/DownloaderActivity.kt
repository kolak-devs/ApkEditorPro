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
            val abi = getABI()
            adapter = DownloaderAdapter(
                mutableListOf(
                    "android-framework.jar" to "$domain/apkeditor/framework/$SDK/android.jar",
                    "aapt" to "$domain/apkeditor/bin/$abi/aapt",
                    "aapt2" to "$domain/apkeditor/bin/$abi/aapt2",
                    "mycp" to "$domain/apkeditor/bin/$abi/mycp",
                    "zipalign" to "$domain/apkeditor/bin/$abi/zipalign",
                    //"aaptz" to "DOMAIN/apkeditor/bin/aaptz",
                    "testkey.pk8" to "$domain/apkeditor/keys/testkey.pk8",
                    "testkey.x509.pem" to "$domain/apkeditor/keys/testkey.x509.pem",
                )
            )
        }
    }

    private fun getABI(): String {
        for (androidArch in Build.SUPPORTED_ABIS) {
            return when (androidArch) {
                "arm64-v8a" -> return "arm64-v8a"
                "armeabi-v7a" -> return "armeabi-v7a"
                "x86_64" -> return "x86_64"
                "x86" -> return "x86"
                else -> "armeabi-v7a"
            }
        }
        return "armeabi-v7a"
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
        private const val SDK = 33
    }
}
