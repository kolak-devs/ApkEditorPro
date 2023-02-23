package com.mcal.common.extension

import android.content.Context

import androidx.datastore.preferences.preferencesDataStore

val Context.prefStore by preferencesDataStore(
    name = "ApkEditor_DataStore"
)