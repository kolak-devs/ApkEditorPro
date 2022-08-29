package com.mcal.apkeditor.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.mcal.apkeditor.R
import com.mcal.apkeditor.fragments.SettingsFragment
import com.mcal.common.activities.CustomizedLangActivity
import ru.svolf.melissa.swipeback.SwipeBackActivity

class SettingsActivity : SwipeBackActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        initFullScreen()
        setupToolbar(getString(R.string.settings))
        if(supportFragmentManager.fragments.isEmpty()) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.frame_container, SettingsFragment())
                .commit()
        }
    }

    private fun setupToolbar(title: String?) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (title != null) {
            supportActionBar!!.title = title
        }
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
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