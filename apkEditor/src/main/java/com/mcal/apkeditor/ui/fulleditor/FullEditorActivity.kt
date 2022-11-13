package com.mcal.apkeditor.ui.fulleditor

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.mcal.apkeditor.ApkParseConsumer
import com.mcal.apkeditor.R
import com.mcal.apkeditor.databinding.ActivityFulleditorBinding
import com.mcal.apkeditor.ui.fulleditor.utils.TaskDecoder
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.getDecodeDirectory
import com.mcal.common.utils.ActivityHelper
import com.mcal.common.view.ProgressDialog
import java.io.File

class FullEditorActivity : CustomizedLangActivity(), ApkParseConsumer {
    private lateinit var binding: ActivityFulleditorBinding
    private lateinit var viewModel: FullEditorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFulleditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(R.id.toolbar, "ApkEditor", false)
        initViewModel()
        initNavigation()
        startDecodeApk()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[FullEditorViewModel::class.java]
        viewModel.decodedPath = ActivityHelper.getParam(intent, "decodeRootPath")
        if (viewModel.decodedPath == null) {
            val decodeDir = getDecodeDirectory()
            if (decodeDir != null) {
                viewModel.decodedPath = "$decodeDir/decoded"
            } else {
                viewModel.decodedPath = ScopedStorage.getDecodedDir().path
            }
        }
        viewModel.apkPath = ActivityHelper.getParam(intent, "apkPath")
        viewModel.curConfig = ActivityHelper.getParam(intent, "curConfig")
        viewModel.allStringValuesFile = ActivityHelper.getParam(intent, "allStringValues_file")
        viewModel.changedStringValuesFile = ActivityHelper.getParam(intent, "changedStringValues_file")
    }

    private fun startDecodeApk() {
        ProgressDialog(
            this, "Decompiling", "Please wait...", false,
            object : ProgressDialog.ProcessingInterface {
                @Throws(Exception::class)
                override fun process() {
                    val apkPath = viewModel.apkPath
                    val decodedPath = viewModel.decodedPath

                    if (apkPath != null && decodedPath != null) {
                        TaskDecoder().decode(this@FullEditorActivity, File(apkPath), File(decodedPath))
                    }
                }

                override fun afterProcess() = Unit
            }, -1
        ).show()
    }

    private fun initNavigation() {
        val appBarConfiguration: AppBarConfiguration = AppBarConfiguration.Builder(R.id.navigation_strings, R.id.navigation_resources, R.id.navigation_manifest).build()
        val navController: NavController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main)
        setupActionBarWithNavController(this, navController, appBarConfiguration)
        setupWithNavController(binding.navView, navController)
    }

    override fun decodeFailed(errMessage: String?) {
        viewModel.decodedFailed = errMessage
    }

    override fun resTableDecoded(result: Boolean) {
        viewModel.isResTableDecoded = result
    }

    override fun resourceDecoded(fileEntry2ZipEntry: MutableMap<String, String>?) {
        viewModel.fileEntry2ZipEntry = fileEntry2ZipEntry
    }
}