package com.mcal.apkeditor.settings.presentation.base

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceFragmentCompat
import com.mcal.presentation.base.BaseActivity

abstract class BasePreferenceFragment<VM : ViewModel> : PreferenceFragmentCompat() {

    protected lateinit var viewModel: VM

    protected abstract fun viewModelClass(): Class<VM>

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        viewModel = (requireActivity() as BaseActivity<*, *>).provideViewModel(viewModelClass(), this)
        onSetupLayout(bundle, s)
        onBindViewModel()
    }

    abstract fun onSetupLayout(bundle: Bundle?, s: String?)

    abstract fun onBindViewModel()
}
