package com.mcal.apkeditor.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Process
import android.view.*
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.balsikandar.crashreporter.ui.CrashReporterActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.ApkComposeService
import com.mcal.apkeditor.BuildConfig
import com.mcal.apkeditor.R
import com.mcal.apkeditor.adapters.MainMenuItem
import com.mcal.apkeditor.adapters.MainProjectItem
import com.mcal.apkeditor.databinding.ActivityMainBinding
import com.mcal.apkeditor.dialogs.AppAgreementDialog
import com.mcal.apkeditor.dialogs.AppAgreementDialog.Companion.appLicenseAccepted
import com.mcal.apkeditor.dialogs.selectFullEditDialog
import com.mcal.apkeditor.prj.ProjectListActivity
import com.mcal.apkeditor.utils.Utils
import com.mcal.common.App
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.filesystem.FilePickHelper
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.copyFile
import com.mcal.common.utils.deleteAll
import com.mcal.common.utils.isNetworkAvailable
import com.mcal.common.view.ProgressDialog.ProcessingInterface
import com.mcal.downloader.DownloaderActivity
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess

class MainActivity : CustomizedLangActivity(), ProcessingInterface {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var pickLauncher: ActivityResultLauncher<Intent>

    companion object {
        init {
            System.loadLibrary("apkeditorpro")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(R.id.toolbar, getString(R.string.app_name), false)
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_settings -> {
                        val i = Intent(this@MainActivity, SettingsActivity::class.java)
                        startActivity(i)
                        return true
                    }
                    R.id.action_night_mode -> {
                        lifecycleScope.launch {
                            if (ReactivePreferences.isLegacyNightMode()) {
                                ReactivePreferences.setNightMode(false)
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                                delegate.applyDayNight()
                            } else {
                                ReactivePreferences.setNightMode(true)
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                                delegate.applyDayNight()
                            }
                        }

                        return true
                    }
                }
                return false
            }
        })

        initUI()

        if (BuildConfig.SHOW_AGREEMENT) {
            if (!appLicenseAccepted(this)) {
                AppAgreementDialog(this)
            } else {
                initFileWithPermissionCheck()
            }
        } else {
            initFileWithPermissionCheck()
        }

        pickLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri->
                    val apk = File(ScopedStorage.getTmpDir().path, "app.apk")
                    contentResolver.openInputStream(uri)?.let { inputStream ->
                        copyFile(inputStream, apk)
                    }
                    if (apk.exists() && apk.name.endsWith(".apk")) {
                        selectFullEditDialog(this, apk.path)
                    } else {
                        Toast.makeText(this, R.string.msg_unsupported_file, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        binding.errors.visibility = if (!isNetworkAvailable(this)) {
            binding.errors.text = "Отсутствует Интернет подключение"
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    public override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    private fun initUI() {
        findViewById<MaterialToolbar>(R.id.toolbar).subtitle = Utils.getVersionString()
        val apkItemAdapter = ItemAdapter<MainMenuItem>()
        val fastApkAdapter = FastAdapter.with(apkItemAdapter)

        val projectAdapter = ItemAdapter<MainProjectItem>()
        val fastProjectAdapter = FastAdapter.with(projectAdapter)

        val itemAdapter = ItemAdapter<MainMenuItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        binding.apkRecycler.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = fastApkAdapter
        }
        binding.projectsRecycler.adapter = fastProjectAdapter
        binding.menuRecycler.adapter = fastAdapter

        // id может быть любым числом, главное, чтобы оно было уникальным. Сделано для того, чтобы не ломалась логика
        // onClick при добавлении новых айтемов
        apkItemAdapter.add(
            MainMenuItem(0, R.drawable.ic_android, R.string.select_file),
            MainMenuItem(1, R.drawable.apps_box, R.string.select_app),
        )
        projectAdapter.add(
            MainProjectItem(System.currentTimeMillis().toInt(), BitmapFactory.decodeResource(binding.root.resources, R.drawable.info), "Ебануть"),
            MainProjectItem(System.currentTimeMillis().toInt(), BitmapFactory.decodeResource(binding.root.resources, R.drawable.info), "список"),
            MainProjectItem(System.currentTimeMillis().toInt(), BitmapFactory.decodeResource(binding.root.resources, R.drawable.info), "проектов"),
        )

        itemAdapter.add(
            MainMenuItem(3, R.drawable.puzzle, R.string.odex_patcher),
            MainMenuItem(4, R.drawable.settings, R.string.tools_manager),
            MainMenuItem(5, R.drawable.round_logo_dev_24, R.string.view_logs),
            MainMenuItem(6, R.drawable.ic_exit_to_app, R.string.exit)
        )

        fastApkAdapter.onClickListener = { _: View?, _: IAdapter<MainMenuItem>, mainMenuItem: MainMenuItem, i: Int ->
            when (mainMenuItem.id) {
                0 -> {
                    pickApk()
                    true
                }
                1 -> {
                    val intent = Intent(this, UserAppActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        fastAdapter.onClickListener =
            { _: View?, _: IAdapter<MainMenuItem>, mainMenuItem: MainMenuItem, i: Int ->
                when (mainMenuItem.id) {
                    3 -> {
                        val intent = Intent(this, OdexPatchActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    4 -> {
                        val intent = Intent(this, DownloaderActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    5 -> {
                        val intent = Intent(this, CrashReporterActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    6 -> {
                        Process.killProcess(Process.myPid())
                        exitProcess(0)
                    }
                    else -> false
                }
            }

//        val msg = findViewById<TextView>(R.id.pirated_version_detected)
//        if (BuildConfig.DEBUG || Native.getSignature(this)
//                .startsWith("kQpOVghQhe8XLbkzKM4PynXi8R0=")
//        ) {
//            msg.visibility = View.INVISIBLE
//        } else {
//            msg.visibility = View.INVISIBLE
//        }
        if (!ScopedStorage.isToolsInstalled()) {
            showToolManagerDialog()
        }
    }

    private fun pickApk() {
        pickLauncher.launch(FilePickHelper.pickFile(true))
    }

    private fun showToolManagerDialog() {
        val context = this@MainActivity
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val padding = App.dp2px(16f, context).toInt()
        val container = LinearLayout(context).apply {
            layoutParams = params
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, 0)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.dialog_install_frameworks))
            .setMessage(getString(R.string.dialog_install_frameworks_sum))
            .setView(container)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val intent = Intent(context, DownloaderActivity::class.java)
                startActivity(intent)
                dialog.dismiss()
            }.show()
    }

    override fun onBackPressed() {
        finishAfterTransition()
    }

    fun initFileWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }
    }

    @Throws(Exception::class)
    override fun process() {
        try {
            val intent = Intent(this, ApkComposeService::class.java)
            stopService(intent)
            val fileDir = filesDir
            val rootDirectory = fileDir.absolutePath
            val decodeRootPath = "$rootDirectory/decoded"
            deleteAll(File(decodeRootPath))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun afterProcess() {
        Process.killProcess(Process.myPid())
    }
}