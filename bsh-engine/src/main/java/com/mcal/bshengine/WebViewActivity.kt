package com.mcal.bshengine

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mcal.bshengine.databinding.WebviewActivityBinding
import com.mcal.bshengine.utils.FileReader
import com.mcal.bshengine.utils.HtmlRenderer
import com.mcal.common.activities.CustomizedLangActivity
import kotlinx.coroutines.*

class WebViewActivity : CustomizedLangActivity() {
    private lateinit var binding: WebviewActivityBinding

    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WebviewActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(R.id.toolbar, "Documentation", back = true)
        val webView = binding.webView
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = ChromeClient(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.allowUniversalAccessFromFileURLs = true
        val link = "https://timscriptov.ru/apkeditor/doc/bsh-patcher/index-v1.html"
        CoroutineScope(Dispatchers.IO).launch {
            val async = async {
                HtmlRenderer.renderHtml(FileReader.fromUrl(link))
            }
            val result = async.await()
            withContext(Dispatchers.Main) {
                webView.loadDataWithBaseURL(link, result, "text/html", "UTF-8", link)
            }
        }
    }

    private class ChromeClient(val activity: CustomizedLangActivity) : WebChromeClient() {
        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            activity.setupToolbar(R.id.toolbar, title, back = true)
        }
    }
}