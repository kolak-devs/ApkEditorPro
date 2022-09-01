package com.mcal.apkeditor.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import com.mcal.apkeditor.R
import com.mcal.common.activities.CustomizedLangActivity
import ru.svolf.melissa.swipeback.SwipeBackActivity

class AboutActivity : CustomizedLangActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setupToolbar(R.string.about)
    }

    private fun setupToolbar(@StringRes title: Int) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setTitle(title)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    fun openTelegram(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/apkeditorproofficial")))
    }

    fun openGitHub(view: View) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/TimScriptov/ApkEditor")
            )
        )
    }

    fun donateTon(view: View) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(
            "Copied TON Coin Address",
            "EQBw0AcMsqJ7slxDD8u8bo2frsqWizASHLHmlNkte6giZWBE"
        )
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied TON Coin Address", Toast.LENGTH_SHORT).show()
    }

    fun donateYandexMoney(view: View) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Yandex Money Address", "4100117726163824")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied Yandex Money Address", Toast.LENGTH_SHORT).show()
    }

    fun donateQiwi(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://qiwi.com/p/79025916451")))
    }

    fun donatePayPal(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/timscriptov")))
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