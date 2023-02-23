package com.mcal.common

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.mcal.Navigator
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.utils.LocaleManager.apply
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class App : Application(), Navigator {

    override fun onCreate() {
        super.onCreate()
        context = this
        CoroutineScope(Dispatchers.Main).launch {
            // Support android 12 Monet Engine
            DynamicColors.applyToActivitiesIfAvailable(this@App)

            if (ReactivePreferences.isNightMode()) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            apply()
        }
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
