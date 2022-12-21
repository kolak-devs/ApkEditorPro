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
    private var _binding: DownloaderActivityBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = DownloaderActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(R.id.toolbar, getString(R.string.tools_manager), true)
        binding.recyclerview.apply {
            layoutManager = LinearLayoutManager(this@DownloaderActivity)
            adapter = DownloaderAdapter(
                listOf(
                    "android-framework.jar" to "${getDomain()}/apkeditor/framework/$SDK/android.jar",
                    "aapt" to "${getDomain()}/apkeditor/bin/$ABI/aapt",
                    "aapt2" to "${getDomain()}/apkeditor/bin/$ABI/aapt2",
                    "mycp" to "${getDomain()}/apkeditor/bin/$ABI/mycp",
                    "zipalign" to "${getDomain()}/apkeditor/bin/$ABI/zipalign",
                    //"aaptz" to "DOMAIN/apkeditor/bin/aaptz",
                    "testkey.pk8" to "${getDomain()}/apkeditor/keys/testkey.pk8",
                    "testkey.x509.pem" to "${getDomain()}/apkeditor/keys/testkey.x509.pem",
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        binding.errors.visibility = if (!isNetworkAvailable(this)) {
            binding.errors.text = getString(R.string.network_available)
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
        private val ABI = Build.SUPPORTED_64_BIT_ABIS[0] ?: Build.SUPPORTED_32_BIT_ABIS[0] ?: "armeabi-v7a"
        private const val SDK = 33
    }
}
