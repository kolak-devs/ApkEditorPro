package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class GlobalConfig {

    private static GlobalConfig config;
    private boolean isFullScreen;

    private GlobalConfig(Context ctx) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(ctx);

        this.isFullScreen = sp.getBoolean("FullScreen", false);
    }

    public static GlobalConfig instance(Context ctx) {
        if (config == null) {
            config = new GlobalConfig(ctx);
        }

        return config;
    }

    public boolean isFullScreen() {
        return isFullScreen;
    }

    public void setFullScreen(Context ctx, boolean isFullScreen) {
        if (isFullScreen != this.isFullScreen) {
            this.isFullScreen = isFullScreen;
            SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(ctx);
            Editor editor = sp.edit();
            editor.putBoolean("FullScreen", isFullScreen);
            editor.apply();
        }
    }
}
