package com.mcal.common.data

import androidx.datastore.preferences.core.edit
import com.mcal.common.App
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object ReactivePreferences {
    suspend fun isNightMode(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Main.UI_THEME] ?: false
    }

    suspend fun setNightMode(enabled: Boolean){
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Main.UI_THEME] = enabled
        }
    }

    suspend fun isMonetEnabled(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Main.UI_MONET] ?: false
    }

    suspend fun isDomainCom(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Main.NET_SERVER].equals("hk")
    }

    suspend fun getAppLanguage(): String {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Main.UI_LANGUAGE] ?: ""
    }

    suspend fun getDecodeDirectory(): String? {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.DECODE_DIR]
    }

    suspend fun getWebViewLanguage(): String {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Misc.WEB_LANGUAGE] ?: "ru"
    }

    suspend fun setWebViewLanguage(language: String) {
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Misc.WEB_LANGUAGE] = language
        }
    }

    suspend fun getFontSize(): Int {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Editor.FONT_SIZE] ?: 14
    }

    suspend fun isWordWrap(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Editor.WORDWRAP] ?: false
    }

    suspend fun isLineNumberEnabled(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Editor.SHOW_LINE_NUMBERS] ?: true
    }

    suspend fun isLineNumberPinned(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Editor.PIN_LINE_NUMBER] ?: false
    }

    suspend fun isMagnifier(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Editor.MAGNIFIER] ?: true
    }

    suspend fun isUseICULibrary(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Editor.USE_ICU_LIB] ?: true
    }

    suspend fun isShowUnprintable(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Editor.SHOW_UNPRINTABLE] ?: true
    }

    suspend fun isAapt2(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.BUILD_USE_AAPT2] ?: true
    }

    suspend fun isFixMultiRes(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.DECODE_MULTIRES] ?: false
    }

    suspend fun isFrameworksInstalled(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.FRAMEWORKS_INSTALLED] ?: false
    }

    suspend fun setFrameworksInstalled(installed: Boolean){
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Compiler.FRAMEWORKS_INSTALLED] = installed
        }
    }

    suspend fun isNeedDecodeAssets(): Boolean{
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.DECODE_ASSETS] ?: false
    }

    suspend fun isNeedDecodeResources(): Boolean{
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.DECODE_RESOURCES] ?: false
    }

    suspend fun isNeedDecodeClasses(): Boolean{
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.DECODE_CLASSES] ?: false
    }

    suspend fun isDexToSmali(): Boolean{
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.BUILD_SMALI_EDITING] ?: true
    }

    suspend fun setDecodeAssets(enabled: Boolean){
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Compiler.DECODE_ASSETS] = enabled
        }
    }

    suspend fun setDecodeResources(enabled: Boolean){
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Compiler.DECODE_RESOURCES] = enabled
        }
    }

    suspend fun setDecodeClasses(enabled: Boolean){
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Compiler.DECODE_CLASSES] = enabled
        }
    }

    suspend fun getKeyPassword(): String {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.SIGNING_KEY_PASS] ?: ""
    }

    suspend fun getKeyAlias(): String {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.SIGNING_KEY_ALIAS] ?: ""
    }

    suspend fun getSigningPassword(): String {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.SIGNING_PASS] ?: ""
    }

    suspend fun isCustomSigningEnabled(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.SIGNING_CUSTOM_ON] ?: false
    }

    suspend fun getSigningVersion(): Int {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.SIGNING_VERSION] ?: 2
    }

    @JvmStatic
    fun isJsonConfig(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.BUILD_JSON_CONFIG] ?: true
        }
        return fallback
    }

    @JvmStatic
    fun isAaptRules(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.BUILD_AAPT_RULES] ?: true
        }
        return fallback
    }

    @JvmStatic
    fun isLegacyNightMode(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = isNightMode()
        }
        return fallback
    }

    @JvmStatic
    fun isLegacySmaliEnabled(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = isDexToSmali()
        }
        return fallback
    }

    @JvmStatic
    fun isLegacyRebuildConfirmation(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.BUILD_CONFIRMATION] ?: false
        }
        return fallback
    }
}