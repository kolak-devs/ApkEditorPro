package com.mcal.apkeditor.fragments

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.R
import com.mcal.common.data.Preferences
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.ScopedStorage.getApkEditorDir
import com.mcal.common.view.ProgressDialog
import com.mcal.common.view.ProgressDialog.ProcessingInterface
import java.io.File


class SettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        newValue: String
    ) {
        if (sharedPreferences.contains("Language")) {
            requireActivity().recreate()
        }
    }

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.settings)

        cleanData()
        cleanHistory()

        // Monet engine is unsupported before A12
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            findPreference<SwitchPreference>("ui_monet")?.isEnabled = false
        }
    }


    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun cleanData() {
        val clean = findPreference<Preference>("CleanGarbage")
        clean?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val builder = MaterialAlertDialogBuilder(requireContext())
                builder.setTitle(R.string.title_clear_data)
                builder.setMessage(R.string.message_clear_data)
                builder.setPositiveButton(android.R.string.ok) { dialog: DialogInterface, _: Int ->
                    ProgressDialog(
                        requireActivity(), "", "Working…", false,
                        object : ProcessingInterface {
                            override fun process() {
                                File(getApkEditorDir().path + "/backups").deleteRecursively()
                                File(getApkEditorDir().path + "/.projects").deleteRecursively()
                                ScopedStorage.getDecodedDir().deleteRecursively()
                            }

                            override fun afterProcess() {}
                        }, R.string.temp_file_cleaned
                    ).show()
                    dialog.cancel()
                }
                builder.setNegativeButton(android.R.string.cancel, null)
                builder.show()
                true
            }
    }

    private fun cleanHistory() {
        val clean = findPreference<Preference>("pref_clear_history")
        clean?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val builder = MaterialAlertDialogBuilder(requireContext())
                builder.setTitle(R.string.title_clear_history)
                builder.setMessage(R.string.message_clear_history)
                builder.setPositiveButton(android.R.string.ok) { dialog: DialogInterface, id: Int ->
                    ProgressDialog(
                        requireActivity(), "", "Working…", false,
                        object : ProcessingInterface {
                            override fun process() {
                                Preferences.setMfKeywordHistory("")
                                Preferences.setStringKeywordHistory("")
                                Preferences.setResKeywordHistory("")
                            }

                            override fun afterProcess() {}
                        }, android.R.string.ok
                    ).show()
                    dialog.cancel()
                }
                builder.setNegativeButton(android.R.string.cancel, null)
                builder.show()
                true
            }
    }
}