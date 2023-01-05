package com.mcal.apkeditor.settings.presentation.screens.apk_settings

import android.os.Bundle
import com.mcal.apkeditor.R
import com.mcal.apkeditor.settings.presentation.base.BasePreferenceFragment
import com.mcal.common.data.BridgeDataStore
import com.mcal.common.data.prefStore


class ApkSettingsFragment : BasePreferenceFragment<ApkSettingsViewModel>() {

    override fun viewModelClass() = ApkSettingsViewModel::class.java

    override fun onSetupLayout(bundle: Bundle?, s: String?) {
        val customStore = BridgeDataStore()
        customStore.attachDataStore(requireContext().prefStore)
        preferenceManager.preferenceDataStore = customStore
        addPreferencesFromResource(R.xml.decode_settings)
    }

    override fun onBindViewModel() = Unit

}
