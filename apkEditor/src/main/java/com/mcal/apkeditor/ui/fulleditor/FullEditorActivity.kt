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


class FullEditorActivity : CustomizedLangActivity() {
    private lateinit var binding: ActivityFulleditorBinding
    private lateinit var viewModel: FullEditorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFulleditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[FullEditorViewModel::class.java]

        setupToolbar(R.id.toolbar, "ApkEditor", false)
        val appBarConfiguration: AppBarConfiguration = AppBarConfiguration.Builder(R.id.navigation_strings, R.id.navigation_resources, R.id.navigation_manifest).build()
        val navController: NavController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main)
        setupActionBarWithNavController(this, navController, appBarConfiguration)
        setupWithNavController(binding.navView, navController)
    }
}