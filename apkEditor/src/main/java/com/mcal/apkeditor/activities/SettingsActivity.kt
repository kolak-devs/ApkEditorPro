package com.mcal.apkeditor.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.mcal.apkeditor.R
import com.mcal.apkeditor.adapters.ViewPagerAdapter
import com.mcal.apkeditor.databinding.ActivitySettingsBinding
import com.mcal.apkeditor.fragments.ApkSettingsFragment
import com.mcal.apkeditor.fragments.SettingsFragment
import com.mcal.apkeditor.fragments.TextSettingsFragment
import com.mcal.common.activities.CustomizedLangActivity

class SettingsActivity : CustomizedLangActivity() {
    private var _binding: ActivitySettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(getString(R.string.settings))

        val pagerAdapter = ViewPagerAdapter(supportFragmentManager)
        pagerAdapter.addFragment(SettingsFragment(), getString(R.string.tab_general))
        pagerAdapter.addFragment(ApkSettingsFragment(), getString(R.string.tab_apk_decoding))
        pagerAdapter.addFragment(TextSettingsFragment(), getString(R.string.tab_text_editor))

        binding.settingsViewpager.adapter = pagerAdapter
        binding.tabLayout.setupWithViewPager(binding.settingsViewpager)

        if (intent != null) {
            val index = intent.getIntExtra("startUpTab", 0)
            binding.settingsViewpager.setCurrentItem(index, false)
        }
    }

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

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}