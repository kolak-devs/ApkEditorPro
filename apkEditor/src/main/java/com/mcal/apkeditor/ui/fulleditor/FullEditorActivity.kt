package com.mcal.apkeditor.ui.fulleditor

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.mcal.apkeditor.R
import com.mcal.apkeditor.databinding.ActivityFulleditorBinding
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.utils.getDecodeDirectory
import com.mcal.common.utilsOld.ActivityUtils

class FullEditorActivity : CustomizedLangActivity() {
    private lateinit var binding: ActivityFulleditorBinding
    private lateinit var viewModel: FullEditorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFulleditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(R.id.toolbar, "ApkEditor", false)
        initViewModel()
        initNav()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[FullEditorViewModel::class.java]
        viewModel.decodedPath = ActivityUtils.getParam(intent, "decodeRootPath")
        if (viewModel.decodedPath == null) {
            val decodeDir = getDecodeDirectory()
            if (decodeDir != null) {
                viewModel.decodedPath = "$decodeDir/decoded"
            } else {
                val fileDir = this.filesDir
                val rootDirectory = fileDir.absolutePath
                viewModel.decodedPath = "$rootDirectory/decoded"
            }
        }
        viewModel.apkPath = ActivityUtils.getParam(intent, "apkPath")
        viewModel.isFullDecoding = ActivityUtils.getBoolParam(intent, "isFullDecoding")
    }

    private fun initNav() {
        val appBarConfiguration: AppBarConfiguration = AppBarConfiguration.Builder(R.id.navigation_strings, R.id.navigation_resources, R.id.navigation_manifest).build()
        val navController: NavController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main)
        setupActionBarWithNavController(this, navController, appBarConfiguration)
        setupWithNavController(binding.navView, navController)
    }
}