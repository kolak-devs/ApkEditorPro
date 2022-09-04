package com.mcal.apkeditor.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import com.mcal.apkeditor.R
import com.mcal.common.activities.CustomizedLangActivity

class HelpActivity : CustomizedLangActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        setupToolbar(R.id.toolbar, getString(R.string.help), true)
        val webView = findViewById<WebView>(R.id.web_view)
        webView.loadUrl("file:///android_res/raw/help.html")
    }
}