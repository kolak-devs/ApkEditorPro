package com.mcal.common.activities

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar
import ru.svolf.melissa.swipeback.SwipeBackActivity
import ru.svolf.melissa.swipeback.SwipeBackLayout
import java.util.*

open class CustomizedLangActivity : SwipeBackActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLanguage()
        setEdgeLevel(SwipeBackLayout.EdgeLevel.MIN)
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

    fun setupToolbar(id: Int, text: String?, back: Boolean) {
        val toolbar = findViewById<MaterialToolbar>(id)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = text
            setDisplayHomeAsUpEnabled(back)
            setDisplayShowHomeEnabled(back)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}