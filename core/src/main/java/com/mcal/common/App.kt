package com.mcal.common

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.mcal.common.utils.LocaleManager.apply
import com.mcal.common.App
import com.balsikandar.crashreporter.CrashReporter
import com.google.android.material.color.DynamicColors
import com.mcal.common.data.Preferences

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        CrashReporter.initialize(this)
        if (Preferences.isNightModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        // Support android 12 Monet Engine
        if (Preferences.isMonetEnabled()) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        apply()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        apply()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var context: Context
        private var preferences: SharedPreferences? = null
        @JvmStatic
        fun getContext(): Context {
            if (context == null) {
                context = App()
            }
            return context
        }

        @JvmStatic
        fun getPreferences(): SharedPreferences? {
            if (preferences == null) {
                preferences = PreferenceManager.getDefaultSharedPreferences(getContext()!!)
            }
            return preferences
        }

        /**
         * This method converts dp unit to equivalent pixels, depending on device density.
         *
         * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
         * @param context Context to get resources and device specific display metrics
         * @return A float value to represent px equivalent to dp depending on device density
         */
        fun dp2px(dp: Float, context: Context): Float {
            return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }

        /**
         * This method converts device specific pixels to density independent pixels.
         *
         * @param px      A value in px (pixels) unit. Which we need to convert into db
         * @param context Context to get resources and device specific display metrics
         * @return A float value to represent dp equivalent to px value
         */
        fun px2dp(context: Context, px: Float): Float {
            return px / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }
    }
}