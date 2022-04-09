package com.mcal.common.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.mcal.common.App;
import com.mcal.common.utils.CommandRunner;
import com.mcal.common.utils.ScopedStorage;

public class Preferences {
    // Some aapt must pass "--no-version-vectors" option to get the correct result
    public static boolean getNoVersionVectorOption(String aaptPath) {
        String configKey = "aapt-no-version-vectors";
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(App.getContext());
        int intVal = sp.getInt(configKey, -1);
        if (intVal == 1) {
            return true;
        } else if (intVal == 0) {
            return false;
        }

        String[] command = {aaptPath};
        CommandRunner cr = new CommandRunner();
        cr.runCommand(command, null, null, 5 * 1000, false);
        String strOut = cr.getStdOut();
        String strError = cr.getStdError();
        boolean option = ((strOut != null && strOut.contains("--no-version-vectors")) ||
                (strError != null && strError.contains("--no-version-vectors")));
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(configKey, option ? 1 : 0);
        editor.apply();
        return option;
    }


    // Assets Installer
    public static boolean getInitialized() {
        return App.getPreferences().getBoolean("initialized", false);
    }

    public static void setInitialized(boolean key) {
        App.getPreferences().edit().putBoolean("initialized", key).apply();
    }

    public static String getVersionString() {
        return App.getPreferences().getString("version", "");
    }

    public static void setVersionString(String key) {
        App.getPreferences().edit().putString("version", key).apply();
    }

    public static boolean isAapt2(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean("aapt2", true);
    }

    /**
     * ApkSigner
     *
     * @return null
     */
    public static String isSignatureKeyType() {
        return App.getPreferences().getString("signatureKey", "0");
    }

    public static String getSignatureAlias() {
        return App.getPreferences().getString("signatureAlias", "");
    }

    public static String getCertPassword() {
        return App.getPreferences().getString("certPass", "");
    }

    public static String getSignaturePassword() {
        return App.getPreferences().getString("signaturePass", "");
    }

    public static String getPk8() {
        return App.getPreferences().getString("pk8Path", "");
    }

    public static void setPk8(String key) {
        App.getPreferences().edit().putString("pk8Path", key).apply();
    }

    public static String getX509() {
        return App.getPreferences().getString("x509Path", "");
    }

    public static void setX509(String key) {
        App.getPreferences().edit().putString("x509Path", key).apply();
    }

    public static String getSignaturePath() {
        return App.getPreferences().getString("signaturePath", "");
    }

    public static void setSignaturePath(String key) {
        App.getPreferences().edit().putString("signaturePath", key).apply();
    }

    public static String getLastDirectory() {
        String rootDir = ScopedStorage.getStorageDirectory().getPath();
        return App.getPreferences().getString("apkDirectory", rootDir);
    }

    public static void setLastDirectory(String directory) {
        App.getPreferences().edit().putString("apkDirectory", directory).apply();
    }

    public static boolean getFullScreen() {
        return App.getPreferences().getBoolean("FullScreen", false);
    }

    public static String getListOrder() {
        return App.getPreferences().getString("AppListOrder", "0");
    }

    public static boolean isFixMultiRes() {
        return App.getPreferences().getBoolean("fixMultiRes", true);
    }

    public static boolean isApkToolCompiler() {
        return App.getPreferences().getBoolean("apkToolCompiler", true);
    }

    // PATCH CODE VIEW
    public static boolean isArtaSyntaxAllowed() {
        return false;
    }

    public static boolean isMonospaceFontAllowed() {
        return true;
    }

    public static int getFontSize() {
        return 12;
    }

    public static boolean isNightModeEnabled() {
        return false;
    }
}
