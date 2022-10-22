package com.mcal.common.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
            binding.errors.visibility = View.VISIBLE
            binding.errors.text = "Не верная ссылка, попробуйте позже"
        }
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

    private class ChromeClient(val activity: CustomizedLangActivity) : WebChromeClient() {
        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            title?.let { activity.setupToolbar(R.id.toolbar, it, back = true) }
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