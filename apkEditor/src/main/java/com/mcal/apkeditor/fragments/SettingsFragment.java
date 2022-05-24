package com.mcal.apkeditor.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.BuildConfig;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.dialogs.ProcessingDialog;
import com.mcal.apksigner.view.CustomSignDialog;
import com.mcal.apksigner.view.CustomSignDialog2;
import com.mcal.common.data.Preferences;
import com.mcal.common.utils.CommandRunner;
import com.mcal.common.utils.RandomUtils;
import com.mcal.common.utils.ScopedStorage;

import java.io.File;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final int EXTRACT_AUTORENAME = 0; // automatically add number suffix
    public static final int EXTRACT_OVERWRITE = 1;
    PreferenceManager manager;
    private Preference cleanKey;

    // Can write to the directory or not
    private static boolean dirCanWrite(String dir) {
        File f = new File(dir);
        if (f.exists() && f.isDirectory()) {
            String rand = RandomUtils.getRandomString(8);
            File tryF = new File(f, rand);
            boolean ret = tryF.mkdir();
            if (ret) {
                tryF.delete();
            }
            return ret;
        }
        return false;
    }

    @Nullable
    public static String getDecodeDirectory(Context ctx) {
        if (BuildConfig.PARSER_ONLY) {
            return ScopedStorage.getStorageDirectory() + "/ApkEditor";
        }

        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(ctx);
        String str = sp.getString("DecodeDirectory", null);

        if (str != null) {
            if (str.endsWith("/")) {
                str = str.substring(0, str.length() - 1);
            }
            if (dirCanWrite(str)) {
                return str;
            }
        }

        return null;
    }

    @NonNull
    public static String getDecodeMode(Context ctx) {
        // "0" means Full Decoding
        if (BuildConfig.PARSER_ONLY) {
            return "0";
        }

        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(ctx);
        String value = sp.getString("DecodeMode", "2");
        if (!value.equals("0") && !value.equals("1") && !value.equals("2")) {
            value = "2";
        }
        return value;
    }

    @Override
    public void onSharedPreferenceChanged(@NonNull SharedPreferences sharedPreferences, String newValue) {
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.settings);
        manager = getPreferenceManager();

        // App list order
        {
            ListPreference orderPref = manager.findPreference("AppListOrder");

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            String defaultOrder = getResources().getStringArray(R.array.order_value)[0];
            String order = sp.getString("AppListOrder", defaultOrder);

            orderPref.setValue(order);
            orderPref.setSummary(order);
        }

        // Output APK Name
        {
            ListPreference outputApkname = manager.findPreference("OutputApkName");
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            String strNameIdx = sp.getString("OutputApkName", "1");
            outputApkname.setValue(strNameIdx);
        }

        // Decode directory
        EditTextPreference dirPref = manager.findPreference("DecodeDirectory");
        //dirPref.setOnPreferenceChangeListener(getContext());
        String decodeDir = getDecodeDirectory(getContext());
        if (decodeDir != null) {
            dirPref.setSummary(decodeDir);
        }

        // Remove APK Building related settings and launcher icon setting
        if (BuildConfig.PARSER_ONLY) {
            PreferenceScreen root = manager.findPreference("ROOT");
            Preference pref = manager.findPreference("ApkBuilding");
            root.removePreference(pref);
        }
        apkSigner();

        cleanData();

        cleanHistory();
    }

    public void apkSigner() {
        ListPreference apkSigner = manager.findPreference("signatureKey");
        apkSigner.setOnPreferenceChangeListener((preference, newValue) -> {
            switch ((String) newValue) {
                case "0":
                    break;
                case "1":
                    new CustomSignDialog(getContext(), null).show();
                    break;
                case "2":
                    new CustomSignDialog2(getContext(), null).show();
                    break;
            }
            return true;
        });
    }

    // Clean the ApkEditor folder except backups
    protected void cleanSdcard() {
        File f = new File(ScopedStorage.getApkEditorDirectory().getPath());
        if (!f.exists() || !f.isDirectory()) {
            return;
        }

        String[] keepingFolders = {"backups", ".projects"};
        File[] subfiles = f.listFiles();
        if (subfiles != null)
            for (File subfile : subfiles) {
                if (subfile.isDirectory()) {
                    boolean bKeep = false;
                    String folder = subfile.getName();
                    for (String keepDir : keepingFolders) {
                        if (folder.equals(keepDir)) {
                            bKeep = true;
                            break;
                        }
                    }
                    if (!bKeep) {
                        cleanDirectory(subfile);
                        subfile.delete();
                    }
                } else {
                    subfile.delete();
                }
            }
    }

    protected void cleanDirectory(@NonNull File dir) {
        File[] subfiles = dir.listFiles();
        if (subfiles != null)
            for (File f : subfiles) {
                if (f.isDirectory()) {
                    cleanDirectory(f);
                    f.delete();
                } else {
                    f.delete();
                }
            }
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        sp.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        sp.registerOnSharedPreferenceChangeListener(this);
    }

    public void cleanData() {
        final String path = getContext().getFilesDir().getAbsolutePath();
        cleanKey = findPreference("CleanGarbage");
        cleanKey.setOnPreferenceClickListener(preference -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
            builder.setTitle(R.string.title_clear_data);
            builder.setMessage(R.string.message_clear_data);
            builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
                ProcessingDialog dlg = new ProcessingDialog(getActivity(),
                        new ProcessingDialog.ProcessingInterface() {
                            @Override
                            public void process() {
                                cleanSdcard();
                                CommandRunner cr = new CommandRunner();
                                cr.runCommand("rm -rf " + path + "/decoded\n"
                                        + "rm -rf " + path + "/temp", null, 8000);
                            }

                            @Override
                            public void afterProcess() {
                            }

                        }, R.string.temp_file_cleaned);
                dlg.show();
                dialog.cancel();
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();

            return true;
        });
    }

    public void cleanHistory() {
        cleanKey = findPreference("pref_clear_history");
        cleanKey.setOnPreferenceClickListener(preference -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
            builder.setTitle(R.string.title_clear_history);
            builder.setMessage(R.string.message_clear_history);
            builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
                ProcessingDialog dlg = new ProcessingDialog(getActivity(),
                        new ProcessingDialog.ProcessingInterface() {
                            @Override
                            public void process() {
                                Preferences.setMfKeywordHistory("");
                                Preferences.setStringKeywordHistory("");
                                Preferences.setResKeywordHistory("");
                            }

                            @Override
                            public void afterProcess() {
                            }

                        }, android.R.string.ok);
                dlg.show();
                dialog.cancel();
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();

            return true;
        });
    }
}