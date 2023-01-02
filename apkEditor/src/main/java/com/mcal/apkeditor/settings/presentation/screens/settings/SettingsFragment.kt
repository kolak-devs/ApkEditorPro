package com.mcal.apkeditor.settings.presentation.screens.settings

import android.os.Build
import android.os.Bundle
import androidx.preference.SwitchPreference
import com.mcal.apkeditor.R
import com.mcal.apkeditor.settings.presentation.base.BasePreferenceFragment
import com.mcal.common.data.BridgeDataStore
import com.mcal.common.data.prefStore


class SettingsFragment : BasePreferenceFragment<SettingsViewModel>() {

    override fun viewModelClass() = SettingsViewModel::class.java

    override fun onSetupLayout(bundle: Bundle?, s: String?) {
        val customStore = BridgeDataStore()
        customStore.attachDataStore(requireContext().prefStore)
        preferenceManager.preferenceDataStore = customStore
        addPreferencesFromResource(R.xml.main_settings)

        // Monet engine is unsupported before A12
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            findPreference<SwitchPreference>("ui_monet")?.isEnabled = false
        }
    }

    override fun onBindViewModel() = Unit

}
