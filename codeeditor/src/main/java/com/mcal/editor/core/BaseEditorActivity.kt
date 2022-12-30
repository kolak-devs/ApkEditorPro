package com.mcal.editor.core

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.viewbinding.ViewBinding
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.sl.ViewModelProvider

typealias Inflate<T> = (LayoutInflater) -> T

abstract class BaseEditorActivity<VM : ViewModel, VB : ViewBinding>(
    private val bindingInflater: Inflate<VB>
) : CustomizedLangActivity(), ViewModelProvider {

    protected lateinit var viewModel: VM

    protected abstract fun viewModelClass(): Class<VM>

    override fun <T : ViewModel> provideViewModel(clazz: Class<T>, owner: ViewModelStoreOwner): T =
        (application as ViewModelProvider).provideViewModel(clazz, owner)

    protected val binding by lazy(LazyThreadSafetyMode.NONE) {
        bindingInflater.invoke(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        viewModel = provideViewModel(viewModelClass(), this)
    }
}
