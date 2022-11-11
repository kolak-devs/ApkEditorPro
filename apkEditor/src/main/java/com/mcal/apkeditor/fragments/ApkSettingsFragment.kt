package com.mcal.apkeditor.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.mcal.apkeditor.R
import com.mcal.common.data.BridgeDataStore
import com.mcal.common.data.prefStore


class ApkSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        val customStore = BridgeDataStore()
        customStore.attachDataStore(requireContext().prefStore)
        preferenceManager.preferenceDataStore = customStore
        addPreferencesFromResource(R.xml.decode_settings)
    }
}