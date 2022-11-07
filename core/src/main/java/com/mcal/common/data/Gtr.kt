package com.mcal.common.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.google.android.gms.common.internal.Objects
import com.mcal.common.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import kotlinx.coroutines.launch

object Gtr {

    fun getPreferences(): DataStore<Preferences> {
        return App.getContext().prefStore
    }

    fun saveBool(key: Preferences.Key<Boolean>, value: Boolean?) {
        CoroutineScope(Dispatchers.IO).launch {
            getPreferences().edit { settings ->
                settings[key] = value ?: false
            }
        }
    }

    fun saveString(key: Preferences.Key<String>, value: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            getPreferences().edit { settings ->
                settings[key] = value ?: ""
            }
        }
    }

    fun saveInt(key: Preferences.Key<Int>, value: Int?) {
        CoroutineScope(Dispatchers.IO).launch {
            getPreferences().edit { settings ->
                settings[key] = value ?: 0
            }
        }
    }

    fun getInt(key: Preferences.Key<Int>, defValue: Int) : Flow<Int> {
        return getPreferences().data.map { preference ->
            val intt = preference[key] ?: defValue
            intt
        }
    }

    fun getBool(key: Preferences.Key<Boolean>, defValue: Boolean) : Flow<Boolean> {
        return getPreferences().data.map { preference ->
            val intt = preference[key] ?: defValue
            intt
        }
    }

    fun getString(key: Preferences.Key<String>, defValue: String) : Flow<String> {
        return getPreferences().data.map { preference ->
            val intt = preference[key] ?: defValue
            intt
        }
    }
}