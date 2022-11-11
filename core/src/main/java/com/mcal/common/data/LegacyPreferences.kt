package com.mcal.common.data

import android.preference.PreferenceManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

import com.mcal.common.App
import com.mcal.common.utils.CommandRunner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object LegacyPreferences {
    // Some aapt must pass "--no-version-vectors" option to get the correct result
    fun getNoVersionVectorOption(aaptPath: String): Boolean {
        val configKey = "aapt-no-version-vectors"
        val sp = PreferenceManager.getDefaultSharedPreferences(App.getContext())
        val intVal = sp.getInt(configKey, -1)
        if (intVal == 1) {
            return true
        } else if (intVal == 0) {
            return false
        }
        val command = arrayOf(aaptPath)
        val cr = CommandRunner()
        cr.runCommand(command, null, null, 5 * 1000, false)
        val strOut = cr.stdOut
        val strError = cr.stdError
        val option = strOut != null && strOut.contains("--no-version-vectors") ||
                strError != null && strError.contains("--no-version-vectors")
        val editor = sp.edit()
        editor.putInt(configKey, if (option) 1 else 0)
        editor.apply()
        return option
    }

    @JvmStatic
    fun getLegacyString(tag: String, defVal: String): String {
        var fallback: String
        runBlocking {
            val key = stringPreferencesKey(tag)
            fallback = App.getContext().prefStore.data.first()[key] ?: defVal
        }
        return fallback
    }

    @JvmStatic
    fun putLegacyString(tag: String, value: String){
        runBlocking {
            val key = stringPreferencesKey(tag)
            App.getContext().prefStore.edit {
                it[key] = value
            }
        }
    }
}