package com.mcal.apkeditor.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

public class SettingEditorActivity {

    public static boolean isLineWrap(Context ctx) {
        String key = "LineWrap";
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean(key, true);
    }

    public static int getFontSize(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return getFontSize(sp);
    }

    public static boolean symbolInputEnabled(Context ctx) {
        String key = "SymbolInput";
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean(key, true);
    }

    private static int getFontSize(@NonNull SharedPreferences sp) {
        String strFontSize = sp.getString("FontSize", "12");
        int fontSize = 12;
        try {
            fontSize = Integer.parseInt(strFontSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fontSize;
    }

    private static int getBigFileThreshold(@NonNull SharedPreferences sp) {
        String strThreshold = sp.getString("BigFileSize", "64");
        int threshold = 64;
        try {
            threshold = Integer.parseInt(strThreshold);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return threshold;
    }

    public static int getBigFileThreshold(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return getBigFileThreshold(sp);
    }

    public static boolean showLineNumbers(Context ctx) {
        String key = "ShowLineNumbers";
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean(key, true);
    }
}