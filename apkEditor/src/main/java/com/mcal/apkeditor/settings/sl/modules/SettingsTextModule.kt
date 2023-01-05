package com.mcal.apkeditor.settings.sl.modules

import com.mcal.apkeditor.settings.presentation.screens.text_settings.TextSettingsViewModel
import com.mcal.sl.Module
import com.mcal.sl.modules.CoreModule

class SettingsTextModule(private val coreModule: CoreModule) : Module<TextSettingsViewModel> {

    override fun viewModel() = TextSettingsViewModel(coreModule.getApplicationContext())
}
