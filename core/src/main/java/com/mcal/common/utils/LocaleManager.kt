package com.mcal.common.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mcal.common.data.Preferences

object LocaleManager {
    @JvmStatic
    fun apply() {
        val language = Preferences.getAppLanguage()
        val locales = LocaleListCompat.forLanguageTags(language)
        if (language.isNullOrEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getDefault())
        } else {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    fun getDocLanguage(): String {
        return when (Preferences.getAppLanguage()) {
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