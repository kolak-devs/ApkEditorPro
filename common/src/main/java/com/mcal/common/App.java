package com.mcal.common;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.balsikandar.crashreporter.CrashReporter;
import com.google.android.material.color.DynamicColors;
import com.mcal.common.data.Preferences;

public class App extends Application {
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    public static SharedPreferences preferences;

    public static Context getContext() {
        if (context == null) {
            context = new App();
        }
        return context;
    }

    public static SharedPreferences getPreferences() {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        }
        return preferences;
    }

    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        CrashReporter.initialize(this);
        if (Preferences.isNightModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        // Support android 12 Monet Engine
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}