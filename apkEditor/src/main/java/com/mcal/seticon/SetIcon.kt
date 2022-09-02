package com.mcal.seticon

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.preference.PreferenceManager
import com.mcal.apkeditor.R

object SetIcon {
    @JvmStatic
    @SuppressLint("NewApi")
    fun setIcon(activity: Activity, iconValue: String) {

        // Const values
        val activityNames = arrayOf(
            "com.mcal.apkeditor.MainActivityNew1",
            "com.mcal.apkeditor.MainActivityNew2",
            "com.mcal.apkeditor.MainActivityNew3",
            "com.mcal.apkeditor.MainActivityNew4",
            "com.mcal.apkeditor.MainActivityNew5",
            "com.mcal.apkeditor.MainActivityNew6",
            "com.mcal.apkeditor.MainActivityNew7",
            "com.mcal.apkeditor.MainActivityNew8",
            "com.mcal.apkeditor.MainActivityNew9",
            "com.mcal.apkeditor.MainActivityNew10",
            "com.mcal.apkeditor.MainActivityNew11",
            "com.mcal.apkeditor.MainActivityNew12",
            "com.mcal.apkeditor.MainActivityNew13",
            "com.mcal.apkeditor.MainActivityNew14",
            "com.mcal.apkeditor.MainActivityNew15",
            "com.mcal.apkeditor.MainActivityNew16",
            "com.mcal.apkeditor.MainActivityNew17"
        )
        val iconIds = allIcons
        val pm = activity.packageManager

        // Disable all activity-aliases
        for (i in activityNames.indices) {
            pm.setComponentEnabledSetting(
                ComponentName(activity, activityNames[i]),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        // Get matched activity
        var matchedIndex = 0
        val iconValues = activity.resources.getStringArray(
            R.array.appicon_value
        )
        for (i in iconValues.indices) {
            if (iconValues[i] == iconValue) {
                matchedIndex = i
                break
            }
        }

        // Enable current selected activity
        pm.setComponentEnabledSetting(
            ComponentName(activity, activityNames[matchedIndex]),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Change ActionBar icon
        activity.actionBar?.setIcon(iconIds[matchedIndex])
    }

    fun getSelectedIcon(activity: Activity): Int {
        val iconIds = allIcons
        val iconValues = activity.resources.getStringArray(
            R.array.appicon_value
        )
        val sp = PreferenceManager.getDefaultSharedPreferences(activity)
        val selected = sp.getString("MyIcon", iconValues[0])
        for (i in iconValues.indices) {
            if (selected == iconValues[i]) {
                return iconIds[i]
            }
        }

        // The first icon as the default
        return iconIds[0]
    }

    val defaultIcon: Int
        get() = R.drawable.editorpro

    @JvmStatic
    val allIcons: IntArray
        get() = intArrayOf(
            R.drawable.editorpro,
            R.drawable.editorpro2,
            R.drawable.appiconframed,
            R.drawable.appiconhex1,
            R.drawable.appiconhex2,
            R.drawable.appiconhex3,
            R.drawable.appiconhex4,
            R.drawable.appiconhex5,
            R.drawable.appiconround_bl,
            R.drawable.appiconround_cy,
            R.drawable.appiconround_gr,
            R.drawable.appiconround_or,
            R.drawable.hexicon1,
            R.drawable.hexicon2,
            R.drawable.hexicon3,
            R.drawable.hexicon4,
            R.drawable.hexicon5
        )

    val iconId: Int
        get() = R.drawable.editorpro
}