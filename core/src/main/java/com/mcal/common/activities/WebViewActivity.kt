package com.mcal.common.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.mcal.common.R
import com.mcal.common.databinding.WebviewActivityBinding
import com.mcal.common.utils.FileReader
import com.mcal.common.utils.HtmlRenderer
import com.mcal.common.utils.isNetworkAvailable
import kotlinx.coroutines.*

class WebViewActivity : CustomizedLangActivity() {
    private lateinit var binding: WebviewActivityBinding
    private var mHtmlUrl: String? = null

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
        intent.extras?.getString(HTML_URL)?.let { link ->
            mHtmlUrl = link
            CoroutineScope(Dispatchers.IO).launch {
                val async = async {
                    HtmlRenderer.renderHtml(FileReader.fromUrl(link))
                }
                val result = async.await()
                withContext(Dispatchers.Main) {
                    webView.loadDataWithBaseURL(link, result, "text/html", "UTF-8", link)
                }
            }
        } ?: run {
            Toast.makeText(this, "Не верная ссылка, попробуйте позже", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isNetworkAvailable(this)) {
            Toast.makeText(this, "Отсутствует Интернет подключение", Toast.LENGTH_SHORT).show()
        }
    }

    private class ChromeClient(val activity: CustomizedLangActivity) : WebChromeClient() {
        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            activity.setupToolbar(R.id.toolbar, title, back = true)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mHtmlUrl = savedInstanceState?.getString(HTML_URL)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(HTML_URL, mHtmlUrl)
    }

    companion object {
        const val HTML_URL = "htmlUrl"
    }
}