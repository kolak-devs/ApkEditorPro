package com.mcal.sl.dependencies

import androidx.lifecycle.ViewModel
import com.mcal.apkeditor.settings.sl.SettingsProvider
import com.mcal.editor.sl.CodeEditorProvider
import com.mcal.sl.DependencyContainer
import com.mcal.sl.Module
import com.mcal.sl.modules.CoreModule

class FeaturesDependencyContainer(
    coreModule: CoreModule,
    private val error: DependencyContainer
) : DependencyContainer {

    private val codeEditorProvider = CodeEditorProvider(coreModule, error)
    private val settingsProvider = SettingsProvider(coreModule, error)

    override fun <T : ViewModel> module(clazz: Class<T>): Module<*> =
        when (clazz) {
            in settingsProvider.getViewModels() -> settingsProvider.provide(clazz)
            in codeEditorProvider.getViewModels() -> codeEditorProvider.provide(clazz)
            else -> error.module(clazz)
        }
}
