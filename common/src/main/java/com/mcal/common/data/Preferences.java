package com.mcal.common.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.mcal.common.AppCommon;
import com.mcal.common.utils.CommandRunner;
import com.mcal.common.utils.ScopedStorage;

public class Preferences {
    // Some aapt must pass "--no-version-vectors" option to get the correct result
    public static boolean getNoVersionVectorOption(String aaptPath) {
        String configKey = "aapt-no-version-vectors";
        SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(AppCommon.getContext());
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
        return AppCommon.getPreferences().getBoolean("initialized", false);
    }

    public static void setInitialized(boolean key) {
        AppCommon.getPreferences().edit().putBoolean("initialized", key).apply();
    }

    public static String getVersionString() {
        return AppCommon.getPreferences().getString("version", "");
    }

    public static void setVersionString(String key) {
        AppCommon.getPreferences().edit().putString("version", key).apply();
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
        return AppCommon.getPreferences().getString("signatureKey", "0");
    }

    public static String getSignatureAlias() {
        return AppCommon.getPreferences().getString("signatureAlias", "");
    }

    public static String getCertPassword() {
        return AppCommon.getPreferences().getString("certPass", "");
    }

    public static String getSignaturePassword() {
        return AppCommon.getPreferences().getString("signaturePass", "");
    }

    public static void setSignatureAlias(String key) {
        AppCommon.getPreferences().edit().putString("signatureAlias", key).apply();
    }

    public static void setCertPassword(String key) {
        AppCommon.getPreferences().edit().putString("certPass", key).apply();
    }

    public static void setSignaturePassword(String key) {
        AppCommon.getPreferences().edit().putString("signaturePass", key).apply();
    }

    public static String getPk8() {
        return AppCommon.getPreferences().getString("pk8Path", "");
    }

    public static void setPk8(String key) {
        AppCommon.getPreferences().edit().putString("pk8Path", key).apply();
    }

    public static String getX509() {
        return AppCommon.getPreferences().getString("x509Path", "");
    }

    public static void setX509(String key) {
        AppCommon.getPreferences().edit().putString("x509Path", key).apply();
    }

    public static String getSignaturePath() {
        return AppCommon.getPreferences().getString("signaturePath", "");
    }

    public static void setSignaturePath(String key) {
        AppCommon.getPreferences().edit().putString("signaturePath", key).apply();
    }


    /**
     *
     * @return
     */
    public static String getLastDirectory() {
        String rootDir = ScopedStorage.getStorageDirectory().getPath();
        return AppCommon.getPreferences().getString("apkDirectory", rootDir);
    }

    public static void setLastDirectory(String directory) {
        AppCommon.getPreferences().edit().putString("apkDirectory", directory).apply();
    }

    public static boolean getFullScreen() {
        return AppCommon.getPreferences().getBoolean("FullScreen", false);
    }

    public static String getListOrder() {
        return AppCommon.getPreferences().getString("AppListOrder", "0");
    }

    public static boolean isFixMultiRes() {
        return AppCommon.getPreferences().getBoolean("fixMultiRes", true);
    }

    public static boolean isApkToolCompiler() {
        return AppCommon.getPreferences().getBoolean("apkToolCompiler", true);
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
        return AppCommon.getPreferences().getBoolean("night_mode", false);
    }

    public static void setNightModeEnabled(boolean flag) {
        AppCommon.getPreferences().edit().putBoolean("night_mode", flag).apply();
    }

    /**
     * SEARCH HISTORY
     */
    public static void setResKeywordHistory(String directory) {
        AppCommon.getPreferences().edit().putString("res_keywords", directory).apply();
    }

    public static void setMfKeywordHistory(String directory) {
        AppCommon.getPreferences().edit().putString("mf_keywords", directory).apply();
    }

    public static void setStringKeywordHistory(String directory) {
        AppCommon.getPreferences().edit().putString("string_keywords", directory).apply();
    }

    public static boolean isSystemShell() {
        // KEY_TERMINAL_USE_SYSTEM_SHELL
        return true;
    }

    public static int getLogSenderVersion() {
        //KEY_LOG_SENDER_VERSION
        return AppCommon.getPreferences().getInt("tools_logsenderVersion", 0);
    }

    public static void setLogSenderVersion(int mode) {
        AppCommon.getPreferences().edit().putInt("mf_keywords", mode).apply();
    }

    public static int getFontSizeTerminal() {
        //KEY_LOG_SENDER_VERSION
        return AppCommon.getPreferences().getInt("terminal_fontSize", 14);
    }

    public static void setFontSizeTerminal(int mode) {
        AppCommon.getPreferences().edit().putInt("terminal_fontSize", mode).apply();
    }
}
