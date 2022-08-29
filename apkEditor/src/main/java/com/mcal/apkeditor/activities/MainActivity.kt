package com.mcal.apkeditor.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.downloader.DownloaderActivity
import com.balsikandar.crashreporter.ui.CrashReporterActivity
import com.mcal.apkeditor.ApkComposeService
import com.mcal.apkeditor.BuildConfig
import com.mcal.apkeditor.R
import com.mcal.apkeditor.adapters.MainMenuItem
import com.mcal.apkeditor.dialogs.AppAgreementDialog
import com.mcal.apkeditor.dialogs.AppAgreementDialog.Companion.appLicenseAccepted
import com.mcal.apkeditor.prj.ProjectListActivity
import com.mcal.apkeditor.utils.Native
import com.mcal.apkeditor.utils.OnlineMessage
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.data.Preferences
import com.mcal.common.utils.deleteAll
import com.mcal.common.view.ProgressDialog.ProcessingInterface
import com.mcal.httpserver.HttpServiceManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import java.io.File

class MainActivity : CustomizedLangActivity(), ProcessingInterface {
    companion object {
        init {
            System.loadLibrary("apkeditorpro")
        }

        @JvmStatic
        external fun modifyZip(
            target: String?, source: String?, added: String?,
            len1: Int, removed: String?, len2: Int, replaced: String?, len3: Int
        )

        fun upgradedFromOldVersion(ctx: Context): Boolean {
            return !File(ctx.filesDir, "work.xml").exists()
        }
    }

    // Used to show a dialog
    private var prompter: OnlineMessage? = null

    private var mRecycler: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initFullScreen()
        setupToolbar(R.id.toolbar, getString(R.string.app_name), false)
        initUI()

        // As pro has no network access, cannot get online message
        if (!BuildConfig.IS_PRO) {
            prompter = OnlineMessage(this)
        }

        if (BuildConfig.SHOW_AGREEMENT) {
            if (!appLicenseAccepted(this)) {
                AppAgreementDialog(this)
            } else {
                initFileWithPermissionCheck()
            }
        } else {
            initFileWithPermissionCheck()
        }
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        if (!BuildConfig.IS_PRO) {
            prompter?.showMessageDialog()
        }
        super.onResume()
    }

    public override fun onDestroy() {
        super.onDestroy()
    }

    private fun initUI() {
        val itemAdapter = ItemAdapter<MainMenuItem>()
        val fastAdapter = FastAdapter.with(itemAdapter)

        mRecycler = findViewById(R.id.menu_recycler)

        mRecycler?.adapter = fastAdapter
        // id может быть любым числом, главное, чтобы оно было уникальным. Сделано для того, чтобы не ломалась логика
        // onClick при добавлении новых айтемов
        itemAdapter.add(
            MainMenuItem(0, R.drawable.ic_android, R.string.select_apk_file),
            MainMenuItem(1, R.drawable.ic_android, R.string.select_apk_from_app),
            MainMenuItem(2, R.drawable.round_inventory_2_24, R.string.projects),
            MainMenuItem(3, R.drawable.puzzle, R.string.odex_patcher),
            MainMenuItem(4, R.drawable.application_braces, R.string.tools_manager),
            MainMenuItem(5, R.drawable.round_logo_dev_24, R.string.view_logs),
            MainMenuItem(6, R.drawable.ic_help, R.string.help),
            MainMenuItem(7, R.drawable.ic_exit_to_app, R.string.exit)
        )
        fastAdapter.onClickListener = { _: View?, _: IAdapter<MainMenuItem>, mainMenuItem: MainMenuItem, i: Int ->
            when (mainMenuItem.id) {
                0 -> {
                    val intent = Intent(this, FileListActivity::class.java)
                    startActivity(intent)
                    true
                }
                1 -> {
                    val intent = Intent(this, UserAppActivity::class.java)
                    startActivity(intent)
                    true
                }
                2 -> {
                    val intent = Intent(this, ProjectListActivity::class.java)
                    startActivity(intent)
                    true
                }
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
                    val intent = Intent(this, HelpActivity::class.java)
                    startActivity(intent)
                    true
                }
                7 -> {
                    finishAndRemoveTask()
                    true
                }
                else -> false
            }
        }

        val msg = findViewById<TextView>(R.id.pirated_version_detected)
        if (BuildConfig.DEBUG || Native.getSignature(this).startsWith("kQpOVghQhe8XLbkzKM4PynXi8R0=")) {
            msg.visibility = View.VISIBLE
        } else {
            msg.visibility = View.INVISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                val i = Intent(this, SettingsActivity::class.java)
                startActivity(i)
                return true
            }
            R.id.action_night_mode -> {
                if (Preferences.isNightModeEnabled()) {
                    Preferences.setNightModeEnabled(false)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    delegate.applyDayNight()
                } else {
                    Preferences.setNightModeEnabled(true)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    delegate.applyDayNight()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            initFile()
        }
    }

    fun initFileWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
            )
        } else {
            initFile()
        }
    }

    private fun initFile() {
        if (!BuildConfig.IS_PRO) {
            return
        }

        if (BuildConfig.LIMIT_NEW_VERSION) {
            return
        }
    }

    @Throws(Exception::class)
    override fun process() {
        try {
            val intent = Intent(this, ApkComposeService::class.java)
            stopService(intent)
            HttpServiceManager.instance().stopWebService(this)
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