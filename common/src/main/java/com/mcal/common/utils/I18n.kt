package com.mcal.common.utils

import android.content.Context
import com.mcal.common.data.Preferences
import java.util.*

object I18n {
    @JvmStatic
    fun setLanguage(context: Context) {
        val defaultLocale = context.resources.configuration.locale
        val config = context.resources.configuration
        val language = Preferences.getAppLanguage()
        if (language.isNullOrEmpty()) {
            config.setLocale(Locale.getDefault())
        } else {
            config.setLocale(Locale(language))
        }
        if (defaultLocale != config.locale) context.resources.updateConfiguration(
            config,
            context.resources.displayMetrics
        )
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