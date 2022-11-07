package com.mcal.common.data

import android.content.Context

import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope

val Context.prefStore by preferencesDataStore(
    name = "sampingan_data_store"
)