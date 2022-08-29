package com.mcal.apkeditor.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import com.mcal.apkeditor.R
import com.mcal.common.activities.CustomizedLangActivity
import ru.svolf.melissa.swipeback.SwipeBackActivity

class EditorHelpActivity : SwipeBackActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            layoutParams = params
        }
        val toolbar = Toolbar(this)
        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.help)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        ll.addView(toolbar)
        val webView = WebView(this).apply {
            loadUrl("file:///android_res/raw/editor_help.html")
        }
        ll.addView(webView)
        setContentView(ll)
        initFullScreen()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Respond to the action bar's Up/Home button
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}