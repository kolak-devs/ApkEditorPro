package com.mcal.sl.dependencies

import androidx.lifecycle.ViewModel
import com.mcal.editor.sl.CodeEditorProvider
import com.mcal.sl.DependencyContainer
import com.mcal.sl.Module
import com.mcal.sl.modules.CoreModule

class FeaturesDependencyContainer(
    coreModule: CoreModule,
    private val error: DependencyContainer
) : DependencyContainer {

    private val codeEditorProvider = CodeEditorProvider(coreModule, error)

    override fun <T : ViewModel> module(clazz: Class<T>): Module<*> =
        when (clazz) {
            in codeEditorProvider.getViewModels() -> codeEditorProvider.provide(clazz)
            else -> error.module(clazz)
        }
}
