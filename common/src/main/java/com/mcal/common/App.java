package com.mcal.common;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.balsikandar.crashreporter.CrashReporter;
import com.blankj.utilcode.util.FileUtils;
import com.mcal.common.data.Preferences;

import java.io.File;
import java.util.Arrays;

public class App extends Application {
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    public static SharedPreferences preferences;
    private static App instance;

    public static Context getContext() {
        return context;
    }

    public static SharedPreferences getPreferences() {
        return preferences;
    }

    @ColorInt
    public static int getColorFromAttr(@NonNull Context context, @AttrRes int attr) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, typedValue, true)) {
            return typedValue.data;
        } else {
            return Color.WHITE;
        }
    }

    public static App getBaseInstance() {
        return instance;
    }

    public static boolean isAarch64() {
        return Arrays.asList(Build.SUPPORTED_ABIS).contains("arm64-v8a");
    }

    public static boolean isArmv7a() {
        return Arrays.asList(Build.SUPPORTED_ABIS).contains("armeabi-v7a");
    }

    @Nullable
    public static String getArch() {
        if (isAarch64()) {
            return "arm64-v8a";
        } else if (isArmv7a()) {
            return "armeabi-v7a";
        }
        return null;
    }

    public void onCreate() {
        super.onCreate();
        instance = this;
        context = getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        CrashReporter.initialize(this);
        if (Preferences.isNightModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public File getRootDir() {
        return new File(getRootFilesDir(), "home");
    }

    @SuppressLint("SdCardPath")
    public File getRootFilesDir() {
        return mkdirIfNotExits(getFilesDir());
    }

    public static File mkdirIfNotExits(File in) {
        if (in != null && !in.exists()) {
            FileUtils.createOrExistsDir(in);
        }
        return in;
    }
}