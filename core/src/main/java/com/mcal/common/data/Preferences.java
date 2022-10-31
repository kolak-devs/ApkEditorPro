package com.mcal.common.data;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;

import com.mcal.common.App;
import com.mcal.common.utils.CommandRunner;
import com.mcal.common.utils.ScopedStorage;

import java.util.Set;

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

    public static boolean isAapt2() {
        return App.getPreferences().getBoolean("aapt2", true);
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
    public static boolean isMonetEnabled() {
        return App.getPreferences().getBoolean("ui_monet", true);
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

    public static boolean isFrameworksInstalled() {
        return App.getPreferences().getBoolean("FrameworksInstalled", false);
    }

    public static void setFrameworksInstalled(boolean mode) {
        App.getPreferences().edit().putBoolean("FrameworksInstalled", mode).apply();
    }

    public static void setHideSmaliMsgShown(int mode) {
        App.getPreferences().edit().putInt("HideSmaliMsgShown", mode).apply();
    }

    public static int getHideSmaliMsgShown() {
        return App.getPreferences().getInt("HideSmaliMsgShown", 0);
    }

    // Режим декомпиляции
    public static boolean isNeedDecodeAssets() {
        return App.getPreferences().getBoolean("isNeedDecodeAssets", false);
    }

    public static void setDecodeAssets(boolean mode) {
        App.getPreferences().edit().putBoolean("isNeedDecodeAssets", mode).apply();
    }

    public static boolean isNeedDecodeResources() {
        return App.getPreferences().getBoolean("isNeedDecodeResources", true);
    }

    public static void setDecodeResources(boolean mode) {
        App.getPreferences().edit().putBoolean("isNeedDecodeResources", mode).apply();
    }

    public static boolean isNeedDecodeClasses() {
        return App.getPreferences().getBoolean("isNeedDecodeClasses", false);
    }

    public static void setDecodeClasses(boolean mode) {
        App.getPreferences().edit().putBoolean("isNeedDecodeClasses", mode).apply();
    }

    public static boolean isDoNotShowComposeTip() {
        return App.getPreferences().getBoolean("donot_show_compose_tip", false);
    }

    public static void setDoNotShowComposeTip(boolean mode) {
        App.getPreferences().edit().putBoolean("donot_show_compose_tip", mode).apply();
    }

    public static boolean isSmaliLicenseShowed() {
        return App.getPreferences().getBoolean("smali_license_showed", false);
    }

    public static void setSmaliLicenseShowed(boolean mode) {
        App.getPreferences().edit().putBoolean("smali_license_showed", mode).apply();
    }

    public static boolean isApkToolJson() {
        return App.getPreferences().getBoolean("apktool_use_json", true);
    }

    public static void setApkToolJson(boolean mode) {
        App.getPreferences().edit().putBoolean("apktool_use_json", mode).apply();
    }

    public static boolean isAEAAPTRules() {
        return App.getPreferences().getBoolean("ae_aapt_rules", true);
    }

    public static void setAEAAPTRules(boolean mode) {
        App.getPreferences().edit().putBoolean("ae_aapt_rules", mode).apply();
    }

    /**
     * Sora Editor
     */
    public static boolean isWordWrap() {
        return App.getPreferences().getBoolean("editor_wordwrap", false);
    }

    public static void setWordWrap(boolean mode) {
        App.getPreferences().edit().putBoolean("editor_wordwrap", mode).apply();
    }

    public static boolean isLineNumberEnabled() {
        return App.getPreferences().getBoolean("editor_line_number", true);
    }

    public static void setLineNumberEnabled(boolean mode) {
        App.getPreferences().edit().putBoolean("editor_line_number", mode).apply();
    }

    public static boolean isLineNumberPinned() {
        return App.getPreferences().getBoolean("editor_pin_line_number", false);
    }

    public static void setLineNumberPinned(boolean mode) {
        App.getPreferences().edit().putBoolean("editor_pin_line_number", mode).apply();
    }

    public static boolean isMagnifier() {
        return App.getPreferences().getBoolean("editor_magnifier", true);
    }

    public static void setMagnifier(boolean mode) {
        App.getPreferences().edit().putBoolean("editor_magnifier", mode).apply();
    }

    public static boolean isUseICULibrary() {
        return App.getPreferences().getBoolean("editor_use_icu_library", true);
    }

    public static void setUseICULibrary(boolean mode) {
        App.getPreferences().edit().putBoolean("editor_use_icu_library", mode).apply();
    }

    public static int getEditorFontSize() {
        return App.getPreferences().getInt("editor_font_size", 14);
    }

    public static void setEditorFontSize(String key) {
        App.getPreferences().edit().putString("editor_text_size", key).apply();
    }

    public static String getWebViewLanguage() {
        return App.getPreferences().getString("webview_language", "ru");
    }

    public static void setWebViewLanguage(String key) {
        App.getPreferences().edit().putString("webview_language", key).apply();
    }

    public static String getKSPass(){
        return App.getPreferences().getString("signing_pass", "");
    }

    public static String getKSAlias(){
        return App.getPreferences().getString("signing_key_alias", "");
    }

    public static String getKeyPass(){
        return App.getPreferences().getString("signing_key_password", "");
    }

    public static boolean isDomainCom() {
        String domain = App.getPreferences().getString("domain_hk", "hk");
        return domain.equals("hk");
    }

    public static boolean isCustomSigning(){
        return App.getPreferences().getBoolean("signing_on", false);
    }

    public static int getSigningVersion() {
        String ver = App.getPreferences().getString("signing_version", "1");
        return Integer.parseInt(ver);
    }
}
