package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

public class GlobalConfig {

    // Values stored in the "AppTheme" preference
    public static final int THEME_FOLLOW_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private static GlobalConfig config;
    private final Context ctx;
    private int themeId;
    private boolean isFullScreen;

    private GlobalConfig(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        try {
            String str = sp.getString("AppTheme", "0");
            int id = Integer.valueOf(str);
            if (id >= 0 && id <= 2) {
                themeId = id;
            } else {
                themeId = 0;
            }
        } catch (Exception e) {
            this.themeId = 0;
        }

        this.isFullScreen = sp.getBoolean("FullScreen", false);
    }

    public static GlobalConfig instance(Context ctx) {
        if (config == null) {
            config = new GlobalConfig(ctx);
        }

        return config;
    }

    // Maps the stored theme value to an AppCompatDelegate night mode
    public static int themeIdToNightMode(int themeId) {
        switch (themeId) {
            case THEME_LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case THEME_DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case THEME_FOLLOW_SYSTEM:
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    public int getNightMode() {
        return themeIdToNightMode(themeId);
    }

    public boolean isDarkTheme() {
        switch (themeId) {
            case THEME_LIGHT:
                return false;
            case THEME_DARK:
                return true;
            case THEME_FOLLOW_SYSTEM:
            default:
                // Follow the actual system mode
                return (ctx.getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
    }

    public int getThemeId() {
        return themeId;
    }

    public boolean isFullScreen() {
        return isFullScreen;
    }

    public void updateThemeId(int themeId) {
        this.themeId = themeId;
    }

    public void setFullScreen(Context ctx, boolean isFullScreen) {
        if (isFullScreen != this.isFullScreen) {
            this.isFullScreen = isFullScreen;
            SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(ctx);
            Editor editor = sp.edit();
            editor.putBoolean("FullScreen", isFullScreen);
            editor.commit();
        }
    }
}
