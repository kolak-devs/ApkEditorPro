package com.mcal.seticon;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;

import com.mcal.apkeditor.R;

public class SetIcon {

    @SuppressLint("NewApi")
    public static void setIcon(Activity activity, String iconValue) {

        // Const values
        String[] activityNames = {
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
                "com.mcal.apkeditor.MainActivityNew17"};

        int[] iconIds = getAllIcons();

        PackageManager pm = activity.getPackageManager();

        // Disable all activity-aliases
        for (int i = 0; i < activityNames.length; i++) {
            pm.setComponentEnabledSetting(new ComponentName(activity,
                            activityNames[i]),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }

        // Get matched activity
        int matchedIndex = 0;
        String[] iconValues = activity.getResources().getStringArray(
                R.array.appicon_value);
        for (int i = 0; i < iconValues.length; i++) {
            if (iconValues[i].equals(iconValue)) {
                matchedIndex = i;
                break;
            }
        }

        // Enable current selected activity
        pm.setComponentEnabledSetting(new ComponentName(activity,
                        activityNames[matchedIndex]),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        // Change ActionBar icon
        if (Build.VERSION.SDK_INT >= 14) {
            if (activity.getActionBar() != null) {
                activity.getActionBar().setIcon(iconIds[matchedIndex]);
            }
        }
    }

    public static int getSelectedIcon(Activity activity) {
        int[] iconIds = getAllIcons();
        String iconValues[] = activity.getResources().getStringArray(
                R.array.appicon_value);

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(activity);
        String selected = sp.getString("MyIcon", iconValues[0]);

        for (int i = 0; i < iconValues.length; i++) {
            if (selected.equals(iconValues[i])) {
                return iconIds[i];
            }
        }

        // The first icon as the default
        return iconIds[0];
    }

    public static int getDefaultIcon() {
        return R.drawable.editorpro;
    }

    public static int[] getAllIcons() {
        return new int[]{R.drawable.editorpro,
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
                R.drawable.hexicon5};
    }

    public static int getIconId() {
        return R.drawable.editorpro;
    }
}
