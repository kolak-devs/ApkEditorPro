package com.mcal.common.activities

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.MenuProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.common.R
import com.mcal.common.data.Preferences
import com.mcal.common.databinding.WebviewActivityBinding
import com.mcal.common.utils.FileReader
import com.mcal.common.utils.HtmlRenderer
import com.mcal.common.utils.isNetworkAvailable
import kotlinx.coroutines.*

class WebViewActivity : CustomizedLangActivity() {
    private var _binding: WebviewActivityBinding? = null
    private val binding get() = _binding!!

    private var mHtmlUrl: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = WebviewActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(R.id.toolbar, "Documentation", back = true)
        val webView = binding.webView
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = ChromeClient(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.allowUniversalAccessFromFileURLs = true
        val refresh = binding.refresh
        refresh.setOnRefreshListener {
            refresh()
        }
        intent.extras?.getString(HTML_URL)?.let { link ->
            mHtmlUrl = link
            CoroutineScope(Dispatchers.IO).launch {
                val async = async {
                    HtmlRenderer.renderHtml(FileReader.fromUrl(link))
                }
                val result = async.await()
                withContext(Dispatchers.Main) {
                    val finalLink = link + "#googtrans(ru|" + Preferences.getWebViewLanguage() + ")"
                    webView.loadDataWithBaseURL(finalLink, result, "text/html", "UTF-8", finalLink)
                }
            }
        } ?: run {
            binding.errors.visibility = View.VISIBLE
            binding.errors.text = "Не верная ссылка, попробуйте позже"
        }
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_webview, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_webview_language -> {
                        webViewLanguageDialog()
                        return true
                    }
                    R.id.menu_webview_refresh -> {
                        refresh()
                        return true
                    }
                }
                return false
            }
        })
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

    private fun webViewLanguageDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
        val items = arrayOf(
            "Русский",
            "English"
        )
        dialog.setItems(items) { p112: DialogInterface, p2: Int ->
            when (p2) {
                0 -> {
                    Preferences.setWebViewLanguage("ru")
                    p112.dismiss()
                    refresh()
                }
                1 -> {
                    Preferences.setWebViewLanguage("en")
                    p112.dismiss()
                    refresh()
                }
            }
        }
        dialog.create()
        dialog.show()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    fun refresh() {
        val refresh = binding.refresh
        refresh.isRefreshing = true
        recreate()
        refresh.isRefreshing = false
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