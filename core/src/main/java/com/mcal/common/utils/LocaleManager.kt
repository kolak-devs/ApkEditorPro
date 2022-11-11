package com.mcal.common.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mcal.common.App
import com.mcal.common.data.LegacyPreferences
import com.mcal.common.data.ReactivePreferences
import kotlinx.coroutines.runBlocking

object LocaleManager {
    suspend fun apply() {
        val language = ReactivePreferences.getAppLanguage()
        val locales = LocaleListCompat.forLanguageTags(language)
        if (language.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getDefault())
        } else {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    suspend fun getDocLanguage(): String {
        return when (ReactivePreferences.getAppLanguage()) {
            "de" -> {
                "de"
            }
            "es" -> {
                "es"
            }
            "hu" -> {
                "hu"
            }
            "ru" -> {
                "ru"
            }
            else -> {
                "en"
            }
        }
    }
}