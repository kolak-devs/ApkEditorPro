package com.mcal.editor.sl.modules

import com.mcal.editor.presentation.EditorViewModel
import com.mcal.sl.Module
import com.mcal.sl.modules.CoreModule

class MainModule(private val coreModule: CoreModule): Module<EditorViewModel> {

    override fun viewModel() = EditorViewModel(coreModule.getApplicationContext())
}
