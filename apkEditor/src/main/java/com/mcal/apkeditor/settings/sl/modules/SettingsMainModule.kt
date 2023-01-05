package com.mcal.apkeditor.settings.sl.modules

import com.mcal.apkeditor.settings.presentation.SettingsActivityViewModel
import com.mcal.sl.Module
import com.mcal.sl.modules.CoreModule

class SettingsMainModule(private val coreModule: CoreModule) : Module<SettingsActivityViewModel> {

    override fun viewModel() = SettingsActivityViewModel(coreModule.getApplicationContext())
}
