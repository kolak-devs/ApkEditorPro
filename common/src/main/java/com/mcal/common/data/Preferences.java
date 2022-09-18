package com.mcal.common.data;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.preference.PreferenceManager;

import com.mcal.common.App;
import com.mcal.common.BuildConfig;
import com.mcal.common.utils.ScopedStorage;
import com.mcal.common.utilsOld.CommandRunner;

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

    public static void setSignatureAlias(String key) {
        App.getPreferences().edit().putString("signatureAlias", key).apply();
    }

    public static String getCertPassword() {
        return App.getPreferences().getString("certPass", "");
    }

    public static void setCertPassword(String key) {
        App.getPreferences().edit().putString("certPass", key).apply();
    }

    public static String getSignaturePassword() {
        return App.getPreferences().getString("signaturePass", "");
    }

    public static void setSignaturePassword(String key) {
        App.getPreferences().edit().putString("signaturePass", key).apply();
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

    /**
     * OTHERS
     */
    public static String getLastDirectory() {
        String rootDir = ScopedStorage.getStorageDirectory().getPath();
        return App.getPreferences().getString("apkDirectory", rootDir);
    }

    public static void setLastDirectory(String directory) {
        App.getPreferences().edit().putString("apkDirectory", directory).apply();
    }

    public static String getListOrder() {
        return App.getPreferences().getString("AppListOrder", "0");
    }

    public static boolean isFixMultiRes() {
        return App.getPreferences().getBoolean("fixMultiRes", true);
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
        return App.getPreferences().getBoolean("night_mode", false);
    }

    public static void setNightModeEnabled(boolean flag) {
        App.getPreferences().edit().putBoolean("night_mode", flag).apply();
    }

    /**
     * SEARCH HISTORY
     */
    public static void setResKeywordHistory(String directory) {
        App.getPreferences().edit().putString("res_keywords", directory).apply();
    }

    public static void setMfKeywordHistory(String directory) {
        App.getPreferences().edit().putString("mf_keywords", directory).apply();
    }

    public static void setStringKeywordHistory(String directory) {
        App.getPreferences().edit().putString("string_keywords", directory).apply();
    }

    /*
    * Monet Engine (API 31+)
    */
    @TargetApi(Build.VERSION_CODES.S)
    public static boolean isMonetEnabled(){
        return App.getPreferences().getBoolean("ui_monet",true);
    }

    public static String getDecodeDirectory() {
        return App.getPreferences().getString("DecodeDirectory", null);
    }

    public static String getOutputApkName() {
        return App.getPreferences().getString("OutputApkName", "1");
    }

    public static boolean isDex2smaliEnabled() {
        return App.getPreferences().getBoolean("SmaliEditingEnabled", true);
    }

    public static boolean isRebuildConfirmEnabled() {
        return App.getPreferences().getBoolean("RebuildConfirmation", false);
    }

    // 0: Auto (by add a number suffix)
    // 1: overwrite
    public static String getFileRenameOption() {
        return App.getPreferences().getString("FileRenameOption", "1");
    }

    public static String getAppLanguage() {
        return App.getPreferences().getString("Language", "");
    }

    public static boolean isFrameworksInstalled(){
        return App.getPreferences().getBoolean("FrameworksInstalled", false);
    }

    public static void setFrameworksInstalled(boolean mode){
        App.getPreferences().edit().putBoolean("FrameworksInstalled", mode).apply();
    }
}
