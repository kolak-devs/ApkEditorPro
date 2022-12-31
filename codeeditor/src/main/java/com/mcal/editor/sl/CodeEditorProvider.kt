package com.mcal.editor.sl

import androidx.lifecycle.ViewModel
import com.mcal.editor.presentation.EditorViewModel
import com.mcal.editor.sl.modules.MainModule
import com.mcal.sl.DependencyContainer
import com.mcal.sl.modules.CoreModule

class CodeEditorProvider(
    private val coreModule: CoreModule,
    private val error: DependencyContainer
) {

    fun <T : ViewModel> provide(clazz: Class<T>) = when (clazz) {
        EditorViewModel::class.java -> MainModule(coreModule)
        else -> error.module(clazz)
    }

    fun getViewModels(): Array<Class<*>> = arrayOf(
        EditorViewModel::class.java
    )
}
