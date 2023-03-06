package com.mcal.common.data

import androidx.datastore.preferences.core.edit
import com.mcal.common.App
import com.mcal.common.extension.prefStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object ReactivePreferences {
    @JvmStatic
    fun isAnnotationPermission(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = App.getContext().prefStore.data.first()[PreferenceScheme.Permissions.ANNOTATION_PERMISSION] ?: false
        }
        return fallback
    }

    @JvmStatic
    fun setAnnotationPermission(enabled: Boolean) {
        runBlocking {
            App.getContext().prefStore.edit {
                it[PreferenceScheme.Permissions.ANNOTATION_PERMISSION] = enabled
            }
        }
    }

    suspend fun isNightMode(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Main.UI_THEME] ?: false
    }

    @JvmStatic
    fun isNightModeAsync(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = App.getContext().prefStore.data.first()[PreferenceScheme.Main.UI_THEME] ?: false
        }
        return fallback
    }

    suspend fun setNightMode(enabled: Boolean) {
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Main.UI_THEME] = enabled
        }
    }

    suspend fun isDomainCom(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Main.NET_SERVER].equals("hk")
    }

    suspend fun getAppLanguage(): String {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Main.UI_LANGUAGE].orEmpty()
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

    suspend fun setIgnoreCase(enabled: Boolean) {
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Editor.IGNORE_CASE] = enabled
        }
    }

    suspend fun isIgnoreCase(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Editor.IGNORE_CASE] ?: false
    }

    @JvmStatic
    fun isIgnoreCaseAsync(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = App.getContext().prefStore.data.first()[PreferenceScheme.Editor.IGNORE_CASE] ?: false
        }
        return fallback
    }

    suspend fun setUseRegex(enabled: Boolean) {
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Editor.USE_REGEX] = enabled
        }
    }

    suspend fun isUseRegex(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Editor.USE_REGEX] ?: false
    }

    @JvmStatic
    fun isUseRegexAsync(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = App.getContext().prefStore.data.first()[PreferenceScheme.Editor.USE_REGEX] ?: false
        }
        return fallback
    }

    suspend fun isAapt2(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.BUILD_USE_AAPT2] ?: true
    }

    suspend fun setAapt2(enabled: Boolean) {
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Compiler.BUILD_USE_AAPT2] = enabled
        }
    }

    @JvmStatic
    fun setAapt2Async(enabled: Boolean) {
        runBlocking {
            App.getContext().prefStore.edit {
                it[PreferenceScheme.Compiler.BUILD_USE_AAPT2] = enabled
            }
        }
    }

    @JvmStatic
    fun isAapt2Async(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.BUILD_USE_AAPT2] ?: true
        }
        return fallback
    }


    @JvmStatic
    fun ignoreMultiResAsync(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.DECODE_MULTIRES] ?: true
        }
        return fallback
    }
    suspend fun ignoreMultiRes(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.DECODE_MULTIRES] ?: true
    }

    suspend fun setIgnoreMultiRes(enabled: Boolean) {
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Compiler.DECODE_MULTIRES] = enabled
        }
    }

    @JvmStatic
    fun setIgnoreMultiResAsync(enabled: Boolean) {
        runBlocking {
            App.getContext().prefStore.edit {
                it[PreferenceScheme.Compiler.DECODE_MULTIRES] = enabled
            }
        }
    }

    suspend fun isNeedDecodeAssets(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.DECODE_ASSETS] ?: false
    }

    @JvmStatic
    fun isNeedDecodeResourcesAsync(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.DECODE_RESOURCES] ?: true
        }
        return fallback
    }

    suspend fun isNeedDecodeResources(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.DECODE_RESOURCES] ?: false
    }

    suspend fun isNeedDecodeClasses(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.DECODE_CLASSES] ?: false
    }

    suspend fun setDecodeAssets(enabled: Boolean) {
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Compiler.DECODE_ASSETS] = enabled
        }
    }

    suspend fun setDecodeResources(enabled: Boolean) {
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Compiler.DECODE_RESOURCES] = enabled
        }
    }

    suspend fun setDecodeClasses(enabled: Boolean) {
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Compiler.DECODE_CLASSES] = enabled
        }
    }

    suspend fun getKeyPassword(): String {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.SIGNING_KEY_PASS].orEmpty()
    }

    suspend fun getKeyAlias(): String {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.SIGNING_KEY_ALIAS].orEmpty()
    }

    suspend fun getSigningPassword(): String {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.SIGNING_PASS].orEmpty()
    }

    suspend fun isSigningEnabled(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.SIGNING_ENABLED] ?: true
    }

    @JvmStatic
    fun isSigningEnabledAsync(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.SIGNING_ENABLED] ?: true
        }
        return fallback
    }

    suspend fun setSigningEnabled(enabled: Boolean) {
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Compiler.SIGNING_ENABLED] = enabled
        }
    }

    @JvmStatic
    fun setSigningEnabledAsync(enabled: Boolean) {
        runBlocking {
            App.getContext().prefStore.edit {
                it[PreferenceScheme.Compiler.SIGNING_ENABLED] = enabled
            }
        }
    }

    suspend fun isCustomSigningEnabled(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.SIGNING_CUSTOM_ON] ?: false
    }

    suspend fun getSigningVersion(): Int {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.SIGNING_VERSION] ?: 2
    }

    suspend fun getGarbageLimit(): Int {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Main.OTHER_GARBAGE_LMT] ?: 512
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
    suspend fun isAaptRules(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.BUILD_AAPT_RULES] ?: true
    }

    @JvmStatic
    fun isAaptRulesAsync(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.BUILD_AAPT_RULES] ?: true
        }
        return fallback
    }

    suspend fun setAaptRules(enabled: Boolean) {
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Compiler.BUILD_AAPT_RULES] = enabled
        }
    }

    @JvmStatic
    fun setAaptRulesAsync(enabled: Boolean) {
        runBlocking {
            App.getContext().prefStore.edit {
                it[PreferenceScheme.Compiler.BUILD_AAPT_RULES] = enabled
            }
        }
    }

    suspend fun isCheckExistsFilesEnabled(): Boolean {
        return App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.CHECK_EXISTS_FILES] ?: false
    }

    @JvmStatic
    fun isCheckExistsFilesEnabledAsync(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = App.getContext().prefStore.data.first()[PreferenceScheme.Compiler.CHECK_EXISTS_FILES] ?: false
        }
        return fallback
    }

    suspend fun setCheckExistsFilesEnabled(enabled: Boolean) {
        App.getContext().prefStore.edit {
            it[PreferenceScheme.Compiler.CHECK_EXISTS_FILES] = enabled
        }
    }

    @JvmStatic
    fun setCheckExistsFilesEnabledAsync(enabled: Boolean) {
        runBlocking {
            App.getContext().prefStore.edit {
                it[PreferenceScheme.Compiler.CHECK_EXISTS_FILES] = enabled
            }
        }
    }

    @JvmStatic
    fun isLegacyNightMode(): Boolean {
        var fallback: Boolean
        runBlocking {
            fallback = isNightMode()
        }
        return fallback
    }

}