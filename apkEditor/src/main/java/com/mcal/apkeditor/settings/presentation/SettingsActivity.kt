package com.mcal.apkeditor.settings.presentation

import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.google.android.material.tabs.TabLayoutMediator
import com.mcal.apkeditor.R
import com.mcal.apkeditor.adapters.ViewPagerAdapter
import com.mcal.apkeditor.databinding.ActivitySettingsBinding
import com.mcal.apkeditor.settings.presentation.screens.apk_settings.ApkSettingsFragment
import com.mcal.apkeditor.settings.presentation.screens.settings.SettingsFragment
import com.mcal.apkeditor.settings.presentation.screens.text_settings.TextSettingsFragment
import com.mcal.presentation.base.BaseActivity

class SettingsActivity : BaseActivity<SettingsActivityViewModel, ActivitySettingsBinding>(
    ActivitySettingsBinding::inflate
) {

    override fun viewModelClass() = SettingsActivityViewModel::class.java

    override fun callOperations() = Unit

    override fun onSetupLayout() = with(binding) {
        setupToolbar(getString(R.string.settings))

        val pagerAdapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        pagerAdapter.addFragment(SettingsFragment(), getString(R.string.tab_general))
        pagerAdapter.addFragment(ApkSettingsFragment(), getString(R.string.tab_apk_decoding))
        pagerAdapter.addFragment(TextSettingsFragment(), getString(R.string.tab_text_editor))

        settingsViewpager.adapter = pagerAdapter
        TabLayoutMediator(tabLayout, settingsViewpager) { tab, position ->
            tab.text = pagerAdapter.getTabTitle(position)
        }.attach()

        if (intent != null) {
            val index = intent.getIntExtra("startUpTab", 0)
            settingsViewpager.setCurrentItem(index, false)
        }
    }

    override fun onBindViewModel() = Unit

    private fun setupToolbar(title: String?) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (title != null) {
            supportActionBar!!.title = title
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
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
