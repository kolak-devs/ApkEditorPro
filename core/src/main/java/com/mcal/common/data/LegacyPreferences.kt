package com.mcal.common.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mcal.common.App
import com.mcal.common.extension.prefStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object LegacyPreferences {

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