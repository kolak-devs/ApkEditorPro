package com.mcal.apkeditor.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.mcal.apkeditor.R
import com.mcal.apkeditor.utils.Utils
import com.mcal.common.activities.CustomizedLangActivity

class AboutActivity : CustomizedLangActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setupToolbar(id = R.id.toolbar, title = getString(R.string.about), subtitle = Utils.getVersionString(), back = true)
    }

    fun openTelegram(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/apkeditorproofficial")))
    }

    fun openTimscriptov(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TimScriptov")))
    }

    fun openSVolf(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/SnowVolf")))
    }
    
    fun openRull(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/cumaRull")))
    }

    fun openJaDX(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/skylot/jadx")))
    }

    fun openApkTool(view: View) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/iBotPeaches/Apktool")
            )
        )
    }

    fun openSmali(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/JesusFreke/smali")))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}