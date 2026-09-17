package com.mcal.apkeditor.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.ApkComposeService
import com.mcal.apkeditor.BuildConfig
import com.mcal.apkeditor.R
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess

class MainActivity : CustomizedLangActivity(), ProcessingInterface {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var pickLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val TAG = "MainActivity"
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
                    val pickedName = FilePickHelper.getFileName(this, uri) ?: "coping.apk"
                    val tmpApk = File(projectsDir, ".coping_$pickedName")
                    try {
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            copyFile(inputStream, tmpApk)
                        } ?: return@let

                        // Reject truncated / non-zip copies instead of crashing later
                        if (!isValidApk(tmpApk)) {
                            Toast.makeText(this@MainActivity, R.string.msg_unsupported_file, Toast.LENGTH_SHORT).show()
                            return@let
                        }

                        var name = "app"
                        // Получение имени приложения
                        ApkInfoParser().parse(this@MainActivity, tmpApk.path)?.label?.let {
                            name = it
                        }
                        val apk = File(projectsDir, "$name.apk")
                        // Копирование АПК во временное хранилище
                        if (!tmpApk.renameTo(apk)) {
                            copyFile(tmpApk, apk)
                        }
                        // Диалог с выбором режима декомпиляции
                        selectFullEditDialog(this@MainActivity, apk.path)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@MainActivity, R.string.msg_unsupported_file, Toast.LENGTH_SHORT).show()
                    } finally {
                        if (tmpApk.exists()) {
                            tmpApk.delete()
                        }
                    }
                }
            }
        }

        scheduleCleaning()
    }

    public override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    private fun initUI() {
        findViewById<MaterialToolbar>(R.id.toolbar).subtitle = Utils.getVersionString()

        binding.btnApk.setOnClickListener { pickApk() }
        binding.btnApp.setOnClickListener {
            startActivity(Intent(this, UserAppActivity::class.java))
        }
        binding.btnPrj.setOnClickListener {
            startActivity(Intent(this, ProjectListActivity::class.java))
        }
        binding.btnOdex.setOnClickListener {
            startActivity(Intent(this, OdexPatchActivity::class.java))
        }
        binding.btnTools.setOnClickListener {
            startActivity(Intent(this, DownloaderActivity::class.java))
        }
        binding.btnInfo.setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            intent.putExtra("htmlUrl", Constants.getDomain() + "/apkeditor/doc/instructions/index.html")
            startActivity(intent)
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnExit.setOnClickListener {
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }

        if (!ScopedStorage.isToolsInstalled()) {
            showToolManagerDialog()
        }
    }

    private fun pickApk() {
        pickLauncher.launch(FilePickHelper.pickFile(true))
    }

    private fun isValidApk(file: File): Boolean {
        if (!file.exists() || file.length() < 4L) return false
        return try {
            file.inputStream().use { input ->
                val magic = ByteArray(4)
                var offset = 0
                while (offset < magic.size) {
                    val read = input.read(magic, offset, magic.size - offset)
                    if (read < 0) return false
                    offset += read
                }
                magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte() &&
                    ((magic[2] == 0x03.toByte() && magic[3] == 0x04.toByte()) ||
                        (magic[2] == 0x05.toByte() && magic[3] == 0x06.toByte()) ||
                        (magic[2] == 0x07.toByte() && magic[3] == 0x08.toByte()))
            }
        } catch (e: Exception) {
            false
        }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.POST_NOTIFICATIONS), 1
                )
            }
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
            Log.i(TAG, "scheduleCleaning: cache size = $total")
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