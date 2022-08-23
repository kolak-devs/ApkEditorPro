package com.mcal.apkeditor.dialogs

import android.app.Activity
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.R

class AboutPluginDialog(activity: Activity?) {
    init {
        val dialog = MaterialAlertDialogBuilder(activity!!)
        val lParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        val webView = WebView(activity).apply {
            loadUrl("file:///android_res/raw/about_translate_plugin.html")
        }
        val ll = LinearLayout(activity).apply {
            layoutParams = lParams
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            addView(webView)
        }
        dialog.setTitle(R.string.translate_plugin)
        dialog.setView(ll)
        dialog.setPositiveButton("Ok", null)
        dialog.show()
    }
}