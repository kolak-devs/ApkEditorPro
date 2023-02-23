package com.mcal.apkeditor.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.*
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
import com.mcal.apkeditor.settings.presentation.SettingsActivity
import com.mcal.apkeditor.utils.Utils
import com.mcal.common.App
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.data.Constants
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.filesystem.FilePickHelper
import com.mcal.common.utils.*
import com.mcal.common.utils.ScopedStorage.getProjects
import com.mcal.common.view.ProgressDialog.ProcessingInterface
import com.mcal.downloader.DownloaderActivity
import com.mcal.webview.WebViewActivity
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

class MainActivity : CustomizedLangActivity(), ProcessingInterface {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var pickLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val TAG = "MainActivity"

        // id для перехода на основной экран проектов
        private const val REQ_SHOW_ALL = 670;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(id = R.id.toolbar, title = getString(R.string.app_name), back = false)
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_settings -> {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
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
            // Политика конфиденциальности
            if (!appLicenseAccepted(this)) {
                AppAgreementDialog(this)
            } else {
                initFileWithPermissionCheck()
            }
        } else {
            initFileWithPermissionCheck()
        }

        // Выбор АПК файла для полного редактирования
        pickLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    val projectsDir = getProjects()
                    var apk = File(projectsDir, FilePickHelper.getFileName(this, uri))
                    contentResolver.openInputStream(uri)?.let { inputStream ->
                        copyFile(inputStream, apk)
                        var name = "app"
                        // Получение имени приложения
                        ApkInfoParser().parse(this@MainActivity, apk.path)?.label?.let {
                            name = it
                        }
                        val newApkPath = File(projectsDir, "$name.apk")
                        // Копирование АПК во временное хранилище
                        apk.renameTo(newApkPath).also { apk = newApkPath }
                    }.also {
                        if (apk.exists()) {
                            // Диалог с выбором режима декомпиляции
                            selectFullEditDialog(this@MainActivity, apk.path)
                        } else {
                            Toast.makeText(this@MainActivity, R.string.msg_unsupported_file, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        scheduleCleaning()
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        binding.errors.visibility = if (!isNetworkAvailable(this)) {
            binding.errors.setText(R.string.no_internet_connection)
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
            MainMenuItem()
                .withId(0)
                .withIcon(R.drawable.ic_android)
                .withTitle(R.string.select_file),
            MainMenuItem()
                .withId(1)
                .withIcon(R.drawable.apps_box)
                .withTitle(R.string.select_app)
        )

        // TODO: Обновить список если Пользователь нажал "Сохранить как проект"
        getProjects().listFiles()?.let { files ->
            //Обрезаем список до 5 первых элементов
            for (f in files.take(5)) {
                if (f.isFile) continue
                findProjectFile(f.listFiles()) ?: continue
                var icon = ContextCompat.getDrawable(this, R.drawable.ic_android)
                files.forEach { file ->
                    if (file.name.endsWith(".apk") && file.name.replace(".apk", "").contains(f.name)) {
                        ApkInfoParser().parse(this@MainActivity, file.path)?.icon?.let {
                            icon = it
                        }
                    }
                }

                val info = ApkInfoActivity.loadProject(f.path) ?: continue
                val fmt = SimpleDateFormat("EEE, HH:mm")
                val millisDate = Files.getLastModifiedTime(File(info.decodeRootPath).toPath()).toMillis()
                projectAdapter.add(
                    MainProjectItem()
                        .withId(System.currentTimeMillis().toInt())
                        .withIcon(icon)
                        .withTitle(File(info.decodeRootPath).name)
                        .withSubTitle(fmt.format(millisDate))
                )
            }

            if (projectAdapter.adapterItemCount > 0) {
                binding.titleProjects.visibility = View.VISIBLE
                projectAdapter.add(
                    MainProjectItem()
                        .withId(REQ_SHOW_ALL)
                        .withIcon(ContextCompat.getDrawable(this, R.drawable.ic_go_into))
                        .withTitle(getString(R.string.projects_show_all))
                )
            } else {
                binding.titleProjects.visibility = View.GONE
            }
        }

        itemAdapter.add(
            MainMenuItem()
                .withId(3)
                .withIcon(R.drawable.puzzle)
                .withTitle(R.string.odex_patcher),
            MainMenuItem()
                .withId(4)
                .withIcon(R.drawable.settings)
                .withTitle(R.string.tools_manager),
            MainMenuItem()
                .withId(5)
                .withIcon(R.drawable.info)
                .withTitle(R.string.apkeditor_instruction),
            MainMenuItem()
                .withId(6)
                .withIcon(R.drawable.ic_exit_to_app)
                .withTitle(R.string.exit)
        )

        fastApkAdapter.onClickListener = { _: View?, _: IAdapter<MainMenuItem>, mainMenuItem: MainMenuItem, _: Int ->
            when (mainMenuItem.identifier) {
                0L -> {
                    pickApk()
                    true
                }
                1L -> {
                    val intent = Intent(this, UserAppActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        fastProjectAdapter.onClickListener = { _: View?, _: IAdapter<MainProjectItem>, mainProjectItem: MainProjectItem, i: Int ->
            if (mainProjectItem.getId() == REQ_SHOW_ALL) {
                startActivity(Intent(this, ProjectListActivity::class.java))
                true
            } else {
                val intent = Intent(this, ApkInfoExActivity::class.java)
                ActivityHelper.attachParam(intent, "projectName", fastProjectAdapter.getItem(i)?.getTitle())
                startActivity(intent)
                true
            }

        }
        fastAdapter.onClickListener =
            { _: View?, _: IAdapter<MainMenuItem>, mainMenuItem: MainMenuItem, _: Int ->
                when (mainMenuItem.identifier) {
                    3L -> {
                        val intent = Intent(this, OdexPatchActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    4L -> {
                        val intent = Intent(this, DownloaderActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    5L -> {
                        val intent = Intent(this, WebViewActivity::class.java)
                        intent.putExtra("htmlUrl", Constants.getDomain() + "/apkeditor/doc/instructions/index.html")
                        startActivity(intent)
                        true
                    }
                    6L -> {
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

    private fun findProjectFile(files: Array<File>?): File? {
        if (files == null) {
            return null
        }
        for (f in files) {
            if (f.isFile && f.name == "info.bin") {
                return f
            }
        }
        return null
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

    fun initFileWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }
    }

    // Очистка мусора исходя из заданного лимита
    private fun scheduleCleaning() {
        CoroutineScope(Dispatchers.IO).launch {
            // Общий размер всех папок в мегабайтах
            val total = Utils.getFoldersSize(
                ScopedStorage.cacheDir, ScopedStorage.getBackupsDir(),
                ScopedStorage.getDecodedDir(), ScopedStorage.getTmpDir()
            ) / 1000 / 1000
            Log.i(TAG, "scheduleCleaning: cache size = " + total)
            if (total > ReactivePreferences.getGarbageLimit()) {
                Utils.deleteFiles(
                    ScopedStorage.cacheDir, ScopedStorage.getBackupsDir(),
                    ScopedStorage.getDecodedDir(), ScopedStorage.getTmpDir()
                )
            }
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