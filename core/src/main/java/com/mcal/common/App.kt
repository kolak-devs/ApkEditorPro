package com.mcal.common

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.mcal.Navigator
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.utils.LocaleManager.apply
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

abstract class App : Application(), Navigator {

    override fun onCreate() {
        super.onCreate()
        context = this
        // Support android 12 Monet Engine. Harus dipanggil sebelum activity pertama dibuat,
        // kalau tidak temanya baru muncul setelah activity di-recreate.
        registerDynamicColors()

        when (ReactivePreferences.getThemeModeAsync()) {
            "night", "amoled" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "day" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        runBlocking { apply() }
    }

    private fun registerDynamicColors() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (ReactivePreferences.isMonetAsync()) {
                    DynamicColors.applyToActivityIfAvailable(activity)
                }
                if (ReactivePreferences.getThemeModeAsync() == "amoled") {
                    activity.setTheme(R.style.AppTheme_AMOLED)
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        CoroutineScope(Dispatchers.Main).launch {
            apply()
        }
    }

    companion object {
        @JvmStatic
        private var context: Context? = null

        @JvmStatic
        fun getContext(): Context {
            return context!!
        }

        /**
         * This method converts dp unit to equivalent pixels, depending on device density.
         *
         * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
         * @param context Context to get resources and device specific display metrics
         * @return A float value to represent px equivalent to dp depending on device density
         */
        @JvmStatic
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
        @JvmStatic
        fun px2dp(context: Context, px: Float): Float {
            return px / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }
    }
}
