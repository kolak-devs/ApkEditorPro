package com.mcal.apkeditor.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.mcal.apkeditor.R


class ApkSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.decode_settings)
    }
}