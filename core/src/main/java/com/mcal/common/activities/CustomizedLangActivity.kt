package com.mcal.common.activities

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.google.android.material.appbar.MaterialToolbar
import ru.svolf.melissa.swipeback.SwipeBackActivity
import ru.svolf.melissa.swipeback.SwipeBackLayout

open class CustomizedLangActivity : SwipeBackActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //LocaleManager.apply()
        setEdgeLevel(SwipeBackLayout.EdgeLevel.MIN)
    }

    fun setupToolbar(id: Int, title: Int, subtitle: String? = null, icon: Drawable? = null, back: Boolean = false) {
        setupToolbar(id, getString(title), subtitle, icon, back)
    }

    fun setupToolbar(id: Int, title: String? = null, subtitle: String? = null, icon: Drawable? = null, back: Boolean = false) {
        val toolbar = findViewById<MaterialToolbar>(id)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title?.let {
                this.title = it
            }
            subtitle?.let {
                this.subtitle = it
            }
            icon?.let {
                this.setIcon(it)
            }
            if (back) {
                this.setDisplayHomeAsUpEnabled(true)
                this.setDisplayShowHomeEnabled(true)
            }
        }
    }

    fun setVisibility(view: View, mode: Int) {
        if (view.visibility != mode) {
            view.visibility = mode
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}