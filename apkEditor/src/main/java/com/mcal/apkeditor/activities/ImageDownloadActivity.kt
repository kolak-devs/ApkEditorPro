package com.mcal.apkeditor.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebView.HitTestResult
import android.webkit.WebViewClient
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.R
import com.mcal.common.App
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.utils.getTargetNonExistFile
import com.mcal.common.utilsOld.IOUtils
import com.mcal.common.view.ProgressDialog
import com.mcal.common.view.ProgressDialog.ProcessingInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.svolf.melissa.swipeback.SwipeBackActivity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URL
import java.net.URLDecoder

class ImageDownloadActivity : SwipeBackActivity(), ProcessingInterface {
    private var webView: WebView? = null
    private var imageUrl: String? = null

    // Searched keyword
    private var keyword: String? = null

    // Download image to
    private var targetDir: String? = null

    // For Processing Dialog
    private var downloadSucceed = false
    private var downloadPath: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        val intent = intent
        targetDir = intent.getStringExtra("Directory")
        webView?.let { web ->
            web.settings.apply {
                builtInZoomControls = true
                useWideViewPort = true
                loadWithOverviewMode = true
                @Suppress("DEPRECATION")
                savePassword = true
                @Suppress("DEPRECATION")
                saveFormData = true
                javaScriptEnabled = true
                domStorageEnabled = true
            }
            web.webChromeClient = WebChromeClient()
            web.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    extractSearchKeyword(url)
                    return false
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    extractSearchKeyword(request.url.toString())
                    return false
                }
            }

            //webView.loadUrl("https://www.google.com/webhp?tbm=isch");
            web.loadUrl("https://icons8.com")
        }
        showDelayedTip(R.string.download_image_tip1, 500, "download1")
        registerForContextMenu(webView)
    }

    private fun showDelayedTip(messageId: Int, delay: Long, tag: String) {
        val sp = App.getPreferences()
        val toShowTip = sp.getBoolean("show_tip_$tag", true)
        if (toShowTip) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(delay)
                val db = MaterialAlertDialogBuilder(this@ImageDownloadActivity)
                db.setMessage(messageId)
                db.setTitle(R.string.tip)
                db.setPositiveButton(android.R.string.ok, null)
                db.show()

                // Make it show only once
                val editor = sp.edit()
                editor.putBoolean("show_tip_$tag", false)
                editor.apply()
            }
        }
    }

    // Extract keyword from URL like: https://www.google.co.uk/search?q=search+png&btnG=&dcr=0&tbm=isch
    private fun extractSearchKeyword(strUrl: String) {
        var position = strUrl.indexOf("search?q=")
        if (position != -1) {
            var keyword = strUrl.substring(position + 9)
            try {
                keyword = URLDecoder.decode(keyword, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
            position = keyword.indexOf('&')
            if (position != -1) {
                keyword = keyword.substring(0, position)
            }
            val keys = keyword.split(" ").toTypedArray()
            for (key in keys) {
                if ("png" == key || "jpg" == key) {
                    continue
                }
                keyword = key
                break
            }

            // When search URL detected, show download tip
            showDelayedTip(R.string.download_image_tip2, 500, "download2")
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val result = webView!!.hitTestResult
        val handler = MenuItem.OnMenuItemClickListener {
            downloadImage()
            true
        }
        if (result.type == HitTestResult.IMAGE_TYPE ||
            result.type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE
        ) {
            imageUrl = result.extra
            menu.setHeaderTitle(getString(R.string.download) + " " + imageUrl)
            menu.add(0, ID_SAVE_IMAGE, 0, R.string.save_image).setOnMenuItemClickListener(handler)
        }
    }

    private fun downloadImage() {
        ProgressDialog(this, "", "Working…", false, this, -1).show()
    }

    @Throws(Exception::class)
    override fun process() {
        downloadSucceed = false
        var isPng = false
        val tmpPath = "$targetDir/.download.img"
        var input: InputStream? = null
        var output: FileOutputStream? = null
        try {
            val url = URL(imageUrl)
            input = url.content as InputStream

            // To check the folder whether exist
            val tmpFile = File(tmpPath)
            tmpFile.parentFile?.let { dir ->
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
            output = FileOutputStream(tmpPath)
            val pngHeader = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
            val head = ByteArray(8)
            input.read(head)
            output.write(head)
            if (pngHeader.contentEquals(head)) {
                isPng = true
            }
            IOUtils.copy(input, output)
        } finally {
            IOUtils.closeQuietly(input)
            IOUtils.closeQuietly(output)
        }

        // The file name either from searched keyword or from image URL
        imageUrl?.let { url ->
            val name = if (keyword == null) extractNameFromUrl(url) else keyword

            // Rename it to a regular path
            val path = targetDir + "/" + name + if (isPng) ".png" else ".jpg"
            var targetFile = File(path)
            if (targetFile.exists()) {
                targetFile = getTargetNonExistFile(path, false)
            }
            downloadPath = targetFile.path
            val ret = File(tmpPath).renameTo(targetFile)
            if (ret) {
                downloadSucceed = true
            }
        }
    }

    override fun afterProcess() {
        if (downloadSucceed) {
            val message = String.format(getString(R.string.image_saved_to), downloadPath)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (webView!!.canGoBack()) {
                        webView!!.goBack()
                    } else {
                        finish()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private const val ID_SAVE_IMAGE = 1

        // Extract the file name from URL
        private fun extractNameFromUrl(url: String): String {
            val sb = StringBuilder()
            val pos = url.lastIndexOf('/')
            for (i in pos + 1 until url.length) {
                val c = url[i]
                if (c == '.') {
                    break
                }
                if (Character.isLetter(c) || Character.isDigit(c)) {
                    sb.append(c)
                }
            }
            return sb.toString()
        }
    }
}