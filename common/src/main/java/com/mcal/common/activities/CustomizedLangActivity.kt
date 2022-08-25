package com.mcal.common.activities

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar
import com.mcal.common.data.Preferences
import java.util.*

open class CustomizedLangActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLanguage()
    }

    private fun initLanguage() {
        val languageToLoad =
            PreferenceManager.getDefaultSharedPreferences(this).getString("Language", "")
        languageToLoad?.let {
            val locale = Locale(languageToLoad)
            Locale.setDefault(locale)
            val config = Configuration()
            config.setLocale(locale)
            baseContext.resources.updateConfiguration(
                config,
                baseContext.resources.displayMetrics
            )
        }
    }

    fun initFullScreen() {
        if (Preferences.isFullScreen()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val insetsController = window.insetsController
                insetsController?.hide(WindowInsets.Type.statusBars())
            } else {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }
        }
    }

    fun setupToolbar(id: Int, title: String?, back: Boolean) {
        val toolbar = findViewById<MaterialToolbar>(id)
        setSupportActionBar(toolbar)
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(back)
        supportActionBar?.setDisplayShowHomeEnabled(back)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}