package com.mcal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.mcal.common.App
import com.mcal.sl.DependencyContainer
import com.mcal.sl.ViewModelFactory
import com.mcal.sl.dependencies.FeaturesDependencyContainer
import com.mcal.sl.modules.CoreModuleImpl

class MainApp: App(), com.mcal.sl.ViewModelProvider {

    private val coreModule = CoreModuleImpl()

    private lateinit var viewModelsFactory: ViewModelFactory

    override fun onCreate() {
        super.onCreate()
        coreModule.init(this)
        val error = DependencyContainer.Error()
        viewModelsFactory = ViewModelFactory(FeaturesDependencyContainer(coreModule, error))
    }

    override fun <T : ViewModel> provideViewModel(clazz: Class<T>, owner: ViewModelStoreOwner) =
        ViewModelProvider(owner, viewModelsFactory)[clazz]
}
