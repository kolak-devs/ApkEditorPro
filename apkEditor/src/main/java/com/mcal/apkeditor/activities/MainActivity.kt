package com.mcal.apkeditor.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import com.app.downloader.DownloaderActivity
import com.balsikandar.crashreporter.ui.CrashReporterActivity
import com.mcal.apkeditor.ApkComposeService
import com.mcal.apkeditor.BuildConfig
import com.mcal.apkeditor.MenuListAdapter
import com.mcal.apkeditor.R
import com.mcal.apkeditor.data.Dialogs
import com.mcal.apkeditor.dialogs.AppAgreementDialog
import com.mcal.apkeditor.dialogs.AppAgreementDialog.Companion.appLicenseAccepted
import com.mcal.apkeditor.prj.ProjectListActivity
import com.mcal.apkeditor.prj.ProjectListActivity2
import com.mcal.apkeditor.utils.Native
import com.mcal.apkeditor.utils.OnlineMessage
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.data.Preferences
import com.mcal.common.utilsOld.FileUtils
import com.mcal.common.view.ProgressDialog.ProcessingInterface
import com.mcal.httpserver.HttpServiceManager
import java.io.File

class MainActivity : CustomizedLangActivity(), OnItemClickListener, ProcessingInterface {
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

    private var mDrawerLayout: DrawerLayout? = null
    private var mDrawerList: ListView? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private lateinit var thread: Thread
    private lateinit var indicator: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initFullScreen()
        setupToolbar(getString(R.string.app_name))
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

    private fun setupToolbar(title: String) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = title
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        if (!BuildConfig.IS_PRO) {
            prompter?.showMessageDialog()
        }
        super.onResume()
        /*if (preferences.getString("token", "").isNullOrBlank()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            thread = Thread {
                do {
                    val url =
                        URL(
                            "https://timscriptov.ru/apkeditor/user.php?token=" + preferences.getString(
                                "token",
                                ""
                            )
                        )
                    val con = url.openConnection() as HttpURLConnection
                    val json = JSONObject(con.inputStream.bufferedReader().readText())
                    val user = User(
                        json.optInt("id"),
                        json.optString("email"),
                        json.optString("token"),
                        json.optInt("is_vip") > 0
                    )
                    if (user.vip.not()) {
                        runOnUiThread {
                            indicator.text = "Not VIP"
                            indicator.setBackgroundColor(Color.RED)
                        }
                    } else {
                        runOnUiThread {
                            indicator.text = "VIP"
                            indicator.setBackgroundColor(Color.GREEN)
                        }
                    }
                    Thread.sleep(30000)
                } while (thread.isInterrupted.not())
            }
            thread.start()
        }*/
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (::thread.isInitialized) {
            thread.interrupt()
        }
    }

    private fun setupSlidingMenu() {
        supportActionBar?.let { actionBar ->
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayShowHomeEnabled(false)
        }
        mDrawerLayout = findViewById(R.id.drawer_layout)
        mDrawerList = findViewById(R.id.slider_list)
        mDrawerList?.let { list ->
            val adapter = MenuListAdapter(this)
            list.adapter = adapter
            list.onItemClickListener = this
        }
        mDrawerToggle = ActionBarDrawerToggle(
            this, mDrawerLayout,
            R.string.app_name,
            R.string.app_name
        )
        mDrawerToggle?.let { toggle ->
            mDrawerLayout?.addDrawerListener(toggle)
        }
    }

    private fun initUI() {
        if (BuildConfig.PARSER_ONLY) {
            val imageView = findViewById<ImageView>(R.id.logo)
            imageView.setImageResource(R.drawable.parser_logo)
        }

        indicator = findViewById(R.id.indicator)

        // Left sliding menu
        setupSlidingMenu()
        if (BuildConfig.DEBUG || Native.getSignature(this).startsWith("kQpOVghQhe8XLbkzKM4PynXi8R0=")) {
            // Select apk from folder
            val openApkBtn = findViewById<Button>(R.id.tv_select_apkfile)
            openApkBtn.setOnClickListener {
                val intent = Intent(this@MainActivity, FileListActivity::class.java)
                startActivity(intent)
            }

            // Select apk from app
            val openAppBtn = findViewById<Button>(R.id.tv_select_appfile)
            if (BuildConfig.DISPLAY_APP) {
                openAppBtn.setOnClickListener {
                    val intent = Intent(this@MainActivity, UserAppActivity::class.java)
                    startActivity(intent)
                }
            } else {
                openAppBtn.setText(R.string.settings)
                openAppBtn.setOnClickListener {
                    val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                    startActivity(intent)
                }
            }

            // Odex Patcher
            val odexPatcherBtn = findViewById<Button>(R.id.tv_odex_patcher)
            odexPatcherBtn.setOnClickListener {
                val intent = Intent(this@MainActivity, OdexPatchActivity::class.java)
                startActivity(intent)
            }

            // Odex Patcher
            val downloadManager = findViewById<Button>(R.id.download_manager)
            downloadManager.setOnClickListener {
                val intent = Intent(this@MainActivity, DownloaderActivity::class.java)
                startActivity(intent)
            }
        } else {
            val msg = findViewById<TextView>(R.id.pirated_version_detected)
            msg.visibility = View.VISIBLE
        }

        // Exit
        val exitButton = findViewById<Button>(R.id.tv_exit)
        exitButton.setOnClickListener { finish() }

        // Help
        // For APK Parser, use it as 'project'
        val helpButton = findViewById<Button>(R.id.tv_help)
        if (BuildConfig.PARSER_ONLY) {
            helpButton.setText(R.string.projects)
            helpButton.setOnClickListener {
                val cls: Class<*> =
                    if (BuildConfig.PARSER_ONLY) {
                        ProjectListActivity2::class.java
                    } else {
                        ProjectListActivity::class.java
                    }
                val helpIntent = Intent(this@MainActivity, cls)
                startActivity(helpIntent)
            }
        } else {
            helpButton.setOnClickListener {
                val helpIntent = Intent(this@MainActivity, HelpActivity::class.java)
                startActivity(helpIntent)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        mDrawerToggle?.let { toggle ->
            if (toggle.onOptionsItemSelected(item)) {
                return true
            }
        }
        when (item.itemId) {
            R.id.action_settings -> {
                val i = Intent(this, SettingsActivity::class.java)
                startActivity(i)
                return true
            }
            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
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

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        mDrawerList?.let { list ->
            mDrawerLayout?.isDrawerOpen(list)?.let { drawerOpen ->
                menu.findItem(R.id.action_settings).isVisible = !drawerOpen
                menu.findItem(R.id.action_about).isVisible = !drawerOpen
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle?.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle?.onConfigurationChanged(newConfig)
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

    override fun onItemClick(adapterView: AdapterView<*>?, view: View, position: Int, id: Long) {
        when (id.toInt()) {
            MenuListAdapter.ITEM_PROJECT -> {
                val cls: Class<*> =
                    if (BuildConfig.PARSER_ONLY) ProjectListActivity2::class.java else ProjectListActivity::class.java
                val intent = Intent(this, cls)
                startActivity(intent)
            }
            MenuListAdapter.ITEM_SETTING -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            MenuListAdapter.ITEM_IMG_DOWNLOADER -> {
                val intent = Intent(this, ImageDownloadActivity::class.java)
                startActivity(intent)
            }
            MenuListAdapter.ITEM_ABOUT -> {
                Dialogs.about(this)
            }
            MenuListAdapter.ITEM_LOGS -> {
                val intent = Intent(this, CrashReporterActivity::class.java)
                startActivity(intent)
            }
            MenuListAdapter.ITEM_FORUM -> {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://forum.timscriptov.ru/index.php?apkeditor-pro.9/")
                    )
                )
            }
            MenuListAdapter.ITEM_TELEGRAM -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/apkeditor2021")))
            }
        }
        mDrawerList?.let { list ->
            mDrawerLayout?.closeDrawer(list)
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
            FileUtils.deleteAll(File(decodeRootPath))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun afterProcess() {
        Process.killProcess(Process.myPid())
    }
}