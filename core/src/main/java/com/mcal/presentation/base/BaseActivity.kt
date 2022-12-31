package com.mcal.presentation.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.viewbinding.ViewBinding
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.sl.ViewModelProvider

typealias Inflate<T> = (LayoutInflater) -> T

abstract class BaseActivity<VM : ViewModel, VB : ViewBinding>(
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
        callOperations()
        onSetupLayout()
        onBindViewModel()
    }

    /**
     * Здесь происходит заполнение layout, например, клики кнопок, состояния на момент открытия и тд
     * **/
    abstract fun onSetupLayout()

    /**
     * Здесь выполняются операции в ViewModel
     * **/
    abstract fun callOperations()

    /**
     * Здесь обсервятся данные с ViewModel
     * **/
    abstract fun onBindViewModel()
}
