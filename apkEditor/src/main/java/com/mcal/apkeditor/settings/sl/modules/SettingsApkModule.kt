package com.mcal.apkeditor.settings.sl.modules

import com.mcal.apkeditor.settings.presentation.screens.apk_settings.ApkSettingsViewModel
import com.mcal.sl.Module
import com.mcal.sl.modules.CoreModule

class SettingsApkModule(private val coreModule: CoreModule): Module<ApkSettingsViewModel> {

    override fun viewModel() = ApkSettingsViewModel(coreModule.getApplicationContext())
}
