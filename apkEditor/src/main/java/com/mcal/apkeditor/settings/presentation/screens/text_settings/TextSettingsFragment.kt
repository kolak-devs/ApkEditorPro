package com.mcal.apkeditor.settings.presentation.screens.text_settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.mcal.apkeditor.R
import com.mcal.apkeditor.settings.presentation.base.BasePreferenceFragment
import com.mcal.common.data.BridgeDataStore
import com.mcal.common.data.prefStore


class TextSettingsFragment : BasePreferenceFragment<TextSettingsViewModel>() {

    override fun viewModelClass() = TextSettingsViewModel::class.java

    override fun onSetupLayout(bundle: Bundle?, s: String?) {
        val customStore = BridgeDataStore()
        customStore.attachDataStore(requireContext().prefStore)
        preferenceManager.preferenceDataStore = customStore
        addPreferencesFromResource(R.xml.editor_settings)
    }

    override fun onBindViewModel() = Unit

}
