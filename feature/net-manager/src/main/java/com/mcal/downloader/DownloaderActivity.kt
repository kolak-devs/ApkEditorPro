package com.mcal.downloader

import com.mcal.appdm.base.R

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.data.Constants
import com.mcal.common.utils.isNetworkAvailable
import com.mcal.appdm.base.databinding.DownloaderActivityBinding

class DownloaderActivity : CustomizedLangActivity() {
    private lateinit var binding: DownloaderActivityBinding
    private var selectedSdk = 33

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DownloaderActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(id = R.id.toolbar, title = getString(R.string.tools_manager), back = true)
        binding.recyclerview.layoutManager = LinearLayoutManager(this@DownloaderActivity)
        showSdkPicker()
    }

    private fun showSdkPicker() {
        val sdks = Constants.SDK_VERSIONS
        val names = sdks.map { it.toString() }.toTypedArray()
        var checked = selectedSdk - sdks.first
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.netmgr_sdk_title))
            .setSingleChoiceItems(names, checked) { _, which -> checked = which }
            .setPositiveButton(getString(R.string.netmgr_ok)) { _, _ ->
                selectedSdk = sdks.first + checked
                showTools()
            }
            .setNegativeButton(getString(R.string.netmgr_cancel)) { _, _ -> showTools() }
            .setOnCancelListener { showTools() }
            .show()
    }

    private fun showTools() {
        val abi = getABI()
        binding.recyclerview.adapter = DownloaderAdapter(
            mutableListOf(
                "android.jar" to Constants.getMaximoffFramework(selectedSdk),
                "aapt" to Constants.getMaximoffBin(abi, "aapt"),
                "aapt2" to Constants.getMaximoffBin(abi, "aapt2"),
                "zipalign" to Constants.getMaximoffBin(abi, "zipalign"),
            )
        )
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
}
