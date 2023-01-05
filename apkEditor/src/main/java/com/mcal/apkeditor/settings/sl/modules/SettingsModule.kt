package com.mcal.apkeditor.settings.sl.modules

import com.mcal.apkeditor.settings.presentation.screens.settings.SettingsViewModel
import com.mcal.sl.Module
import com.mcal.sl.modules.CoreModule

class SettingsModule(private val coreModule: CoreModule) : Module<SettingsViewModel> {

    override fun viewModel() = SettingsViewModel(coreModule.getApplicationContext())
}
