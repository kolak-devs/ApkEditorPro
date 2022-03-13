package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import com.gmail.heagoo.apkeditor.R;

public class SettingEditorActivity extends PreferenceActivity implements
        OnPreferenceChangeListener {

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

    private static int getFontSize(SharedPreferences sp) {
        String strFontSize = sp.getString("FontSize", "12");
        int fontSize = 12;
        try {
            fontSize = Integer.valueOf(strFontSize);
        } catch (Exception e) {
        }
        return fontSize;
    }

    private static int getBigFileThreshold(SharedPreferences sp) {
        String strThreshold = sp.getString("BigFileSize", "64");
        int threshold = 64;
        try {
            threshold = Integer.valueOf(strThreshold);
        } catch (Exception e) {
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

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        this.addPreferencesFromResource(R.xml.editor_setting);
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    @SuppressWarnings("deprecation")
    private void init() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        // Line wrap
        {
            String key = "LineWrap";
            CheckBoxPreference checkbox = (CheckBoxPreference) findPreference(key);
            checkbox.setOnPreferenceChangeListener(this);
            boolean enabled = sp.getBoolean(key, true);
            if (enabled) {
                checkbox.setSummary(R.string.line_wrap_enabled);
                checkbox.setChecked(true);
            } else {
                checkbox.setSummary(R.string.line_wrap_disabled);
                checkbox.setChecked(false);
            }
        }

        // Font size
        {
            String key = "FontSize";
            EditTextPreference pref = (EditTextPreference) findPreference(key);
            pref.setOnPreferenceChangeListener(this);
            int fontSize = getFontSize(sp);
            pref.setSummary(String.format(
                    this.getString(R.string.font_size_summary), fontSize));
        }

        // Big File Threshold
        {
            String key = "BigFileSize";
            EditTextPreference pref = (EditTextPreference) findPreference(key);
            pref.setOnPreferenceChangeListener(this);
            int filesize = getBigFileThreshold(sp);
            pref.setSummary(String.format(
                    this.getString(R.string.use_bfe_summary), filesize));
        }

        // Line numbers
        {
            String key = "ShowLineNumbers";
            CheckBoxPreference checkbox = (CheckBoxPreference) findPreference(key);
            checkbox.setOnPreferenceChangeListener(this);
            boolean enabled = sp.getBoolean(key, true);
            if (enabled) {
                checkbox.setSummary(R.string.line_numebrs_enabled);
                checkbox.setChecked(true);
            } else {
                checkbox.setSummary(R.string.line_numbers_disabled);
                checkbox.setChecked(false);
            }
        }

        // Symbol Input
        {
            String key = "SymbolInput";
            CheckBoxPreference checkbox = (CheckBoxPreference) findPreference(key);
            boolean enabled = sp.getBoolean(key, true);
            checkbox.setChecked(enabled);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        if ("LineWrap".equals(key)) {
            Boolean b = (Boolean) newValue;
            if (b) {
                preference.setSummary(R.string.line_wrap_enabled);
            } else {
                preference.setSummary(R.string.line_wrap_disabled);
            }
        } else if ("FontSize".equals(key)) {
            String strFontSize = (String) newValue;
            int fontSize;
            try {
                fontSize = Integer.valueOf(strFontSize);
            } catch (Exception e) {
                fontSize = getFontSize(this);
            }
            preference.setSummary(String.format(
                    this.getString(R.string.font_size_summary), fontSize));
        } else if ("BigFileSize".equals(key)) {
            String strSize = (String) newValue;
            int fileSize;
            try {
                fileSize = Integer.valueOf(strSize);
            } catch (Exception e) {
                fileSize = getBigFileThreshold(this);
            }
            preference.setSummary(String.format(
                    this.getString(R.string.use_bfe_summary), fileSize));
        }

        if ("ShowLineNumbers".equals(key)) {
            Boolean b = (Boolean) newValue;
            if (b) {
                preference.setSummary(R.string.line_numebrs_enabled);
            } else {
                preference.setSummary(R.string.line_numbers_disabled);
            }
        }

        return true;
    }


}
