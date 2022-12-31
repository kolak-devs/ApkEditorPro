package com.mcal.apkeditor.settings.sl

import androidx.lifecycle.ViewModel
import com.mcal.apkeditor.settings.presentation.SettingsActivityViewModel
import com.mcal.apkeditor.settings.presentation.screens.apk_settings.ApkSettingsViewModel
import com.mcal.apkeditor.settings.presentation.screens.settings.SettingsViewModel
import com.mcal.apkeditor.settings.presentation.screens.text_settings.TextSettingsViewModel
import com.mcal.apkeditor.settings.sl.modules.SettingsApkModule
import com.mcal.apkeditor.settings.sl.modules.SettingsMainModule
import com.mcal.apkeditor.settings.sl.modules.SettingsModule
import com.mcal.apkeditor.settings.sl.modules.SettingsTextModule
import com.mcal.editor.presentation.EditorViewModel
import com.mcal.editor.sl.modules.MainModule
import com.mcal.sl.DependencyContainer
import com.mcal.sl.modules.CoreModule

class SettingsProvider(
    private val coreModule: CoreModule,
    private val error: DependencyContainer
) {

    fun <T : ViewModel> provide(clazz: Class<T>) = when (clazz) {
        SettingsActivityViewModel::class.java -> SettingsMainModule(coreModule)
        ApkSettingsViewModel::class.java -> SettingsApkModule(coreModule)
        SettingsViewModel::class.java -> SettingsModule(coreModule)
        TextSettingsViewModel::class.java -> SettingsTextModule(coreModule)
        else -> error.module(clazz)
    }

    fun getViewModels(): Array<Class<*>> = arrayOf(
        SettingsActivityViewModel::class.java,
        ApkSettingsViewModel::class.java,
        SettingsViewModel::class.java,
        TextSettingsViewModel::class.java
    )
}
