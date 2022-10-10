package com.mcal.common.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.google.android.material.appbar.MaterialToolbar
import com.mcal.common.utils.LocaleManager
import ru.svolf.melissa.swipeback.SwipeBackActivity
import ru.svolf.melissa.swipeback.SwipeBackLayout

open class CustomizedLangActivity : SwipeBackActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleManager.apply()
        setEdgeLevel(SwipeBackLayout.EdgeLevel.MIN)
    }

    fun setupToolbar(
        id: Int, text: String?, back: Boolean = false,
    ) {
        val toolbar = findViewById<MaterialToolbar>(id)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = text
            setDisplayHomeAsUpEnabled(back)
            setDisplayShowHomeEnabled(back)
        }
    }

    fun setupToolbar(
        id: Int, text: String?, message: String = "",  back: Boolean = false,
    ) {
        val toolbar = findViewById<MaterialToolbar>(id)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = text
            if (message.isNotEmpty()) {
                subtitle = message
            }
            setDisplayHomeAsUpEnabled(back)
            setDisplayShowHomeEnabled(back)
        }
    }

    fun setVisibility(view: View, mode: Int) {
        if(view.visibility!=mode) {
            view.visibility=mode
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}