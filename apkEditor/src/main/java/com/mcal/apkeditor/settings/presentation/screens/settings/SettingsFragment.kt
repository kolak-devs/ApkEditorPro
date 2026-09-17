package com.mcal.apkeditor.settings.presentation.screens.settings

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.SwitchPreference
import com.mcal.apkeditor.R
import com.mcal.apkeditor.settings.presentation.base.BasePreferenceFragment
import com.mcal.common.data.BridgeDataStore
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.extension.prefStore


class SettingsFragment : BasePreferenceFragment<SettingsViewModel>() {

    override fun viewModelClass() = SettingsViewModel::class.java

    override fun onSetupLayout(bundle: Bundle?, s: String?) {
        val customStore = BridgeDataStore(requireContext().prefStore)
        preferenceManager.preferenceDataStore = customStore
        addPreferencesFromResource(R.xml.main_settings)

        findPreference<ListPreference>("ui_theme")?.setOnPreferenceChangeListener { _, newValue ->
            AppCompatDelegate.setDefaultNightMode(
                when (newValue) {
                    "night", "amoled" -> AppCompatDelegate.MODE_NIGHT_YES
                    "day" -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
            requireActivity().recreate()
            true
        }

        findPreference<SwitchPreference>("ui_monet")?.setOnPreferenceChangeListener { _, newValue ->
            ReactivePreferences.setMonetAsync(newValue as Boolean)
            requireActivity().recreate()
            true
        }

        // Monet engine is unsupported before A12
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            findPreference<SwitchPreference>("ui_monet")?.isEnabled = false
        }
    }

    override fun onBindViewModel() = Unit

}
