package com.mcal.apkeditor.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.*
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.AdapterView.OnItemClickListener
import com.google.android.material.appbar.MaterialToolbar
import com.mcal.apkeditor.AppInfo
import com.mcal.apkeditor.AppListAdapter
import com.mcal.apkeditor.BuildConfig
import com.mcal.apkeditor.R
import com.mcal.apkeditor.activities.MainActivity.Companion.upgradedFromOldVersion
import com.mcal.apkeditor.dialogs.EditModeView
import com.mcal.apkeditor.dialogs.EditModeView.IEditModeSelected
import com.mcal.apkeditor.se.SimpleEditActivity
import com.mcal.appdm.PrefOverallActivity
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.data.Preferences
import com.mcal.common.utils.makeBackupDir
import com.mcal.common.utilsOld.ActivityUtils
import com.mcal.common.utilsOld.IOUtils
import com.mcal.common.view.ProgressDialog
import com.mcal.common.view.ProgressDialog.ProcessingInterface
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.*

class UserAppActivity : CustomizedLangActivity(), OnItemClickListener, View.OnClickListener, IEditModeSelected {
    private val handler = MyHandler(this)
    private val allApps: MutableList<AppInfo> = ArrayList()
    private val displayApps: MutableList<AppInfo> = ArrayList()

    // appTypeIndex = 0, show user apps; appTypeIndex = 1, show system apps
    private var appTypeIndex = 0
    private var appOrderIndex = 0 // 0 means by name
    private var appListView: ListView? = null
    private var keywordEdit: EditText? = null
    private var searchBtn: ImageButton? = null
    private var progressBar: ProgressBar? = null
    private var userApps: MenuItem? = null
    private var systemApps: MenuItem? = null
    private var sortByApp: MenuItem? = null
    private var sortByInstallTime: MenuItem? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_applist)
        initFullScreen()
        setupToolbar(getString(R.string.select_apk_from_app))

        // Get default app order
        val strOrder = Preferences.getListOrder()
        appOrderIndex = getOrderIndex(strOrder)
        initUI()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_applist, menu)
        userApps = menu.findItem(R.id.user_apps)
        systemApps = menu.findItem(R.id.system_apps)
        sortByApp = menu.findItem(R.id.sort_by_app)
        sortByInstallTime = menu.findItem(R.id.sort_by_install_time)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.user_apps) {
            reScanAppList(0, appOrderIndex)
            systemApps?.isChecked = false
            userApps?.isChecked = true
        } else if (id == R.id.system_apps) {
            reScanAppList(1, appOrderIndex)
            userApps?.isChecked = false
            systemApps?.isChecked = true
        } else if (item.itemId == R.id.sort_by_app) {
            reScanAppList(appTypeIndex, 0)
            sortByInstallTime?.isChecked = false
            sortByApp?.isChecked = true
        } else if (item.itemId == R.id.sort_by_install_time) {
            reScanAppList(appTypeIndex, 1)
            sortByApp?.isChecked = false
            sortByInstallTime?.isChecked = true
        } else if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        reScanAppList()
    }

    public override fun onDestroy() {
        super.onDestroy()
    }

    fun showAppList(appList: MutableList<AppInfo>?) {
        progressBar?.visibility = View.GONE
        appListView?.visibility = View.VISIBLE
        val adapter = appListView?.adapter as AppListAdapter
        val orders = resources.getStringArray(R.array.order_value)
        appList?.let { list ->
            val strOrder = if (appOrderIndex < orders.size) orders[appOrderIndex] else ""
            adapter.setAppList(list, strOrder)
            adapter.notifyDataSetChanged()
            synchronized(displayApps) {
                displayApps.clear()
                displayApps.addAll(list)
            }
        }

        // Enable the search
        keywordEdit?.isEnabled = true
        searchBtn?.isEnabled = true
    }

    // Get app order index from the string
    private fun getOrderIndex(strOrder: String): Int {
        val orders = resources.getStringArray(R.array.order_value)
        for (i in orders.indices) {
            if (strOrder == orders[i]) {
                return i
            }
        }
        return 0
    }

    private fun initAppList() {
        val adapter = AppListAdapter(this)
        adapter.setAppList(displayApps, Preferences.getListOrder())
        appListView?.adapter = adapter
        appListView?.onItemClickListener = this
        registerForContextMenu(appListView)
    }

    private fun initUI() {
        progressBar = findViewById(R.id.progress_bar)
        appListView = findViewById(R.id.application_list)
        initAppList()
        keywordEdit = findViewById(R.id.et_keyword)
        searchBtn = findViewById(R.id.btn_search)
        searchBtn?.setOnClickListener(this)
    }

    private fun setupToolbar(text: String) {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = text
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun reScanAppList(typeIndex: Int, orderIndex: Int) {
        appTypeIndex = typeIndex
        appOrderIndex = orderIndex
        reScanAppList()
    }

    private fun reScanAppList() {
        Thread {

            // To get the data
            val appList: MutableList<AppInfo> = ArrayList()
            getAppList(appList)

            // To refresh UI
            handler.setAppList(appList)
            handler.sendEmptyMessage(0)
        }.start()
    }

    private fun getAppList(appList: MutableList<AppInfo>) {
        val pm = packageManager
        val appInfoList = pm.getInstalledApplications(0)

        // Show user apps
        if (appTypeIndex == 0) {
            for (ai in appInfoList) {
                if (ai.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                    // When do some recording, to enable following line
                    //if (ai.enabled)
                    appList.add(AppInfo.create(pm, ai))
                }
            }
        } else {
            for (ai in appInfoList) {
                if (ai.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                    appList.add(AppInfo.create(pm, ai))
                }
            }
        }
    }

    private fun getAppInfo(position: Int): AppInfo? {
        var info: AppInfo? = null
        synchronized(displayApps) {
            try {
                info = displayApps[position]
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        return info
    }

    override fun onItemClick(parent: AdapterView<*>?, v: View, position: Int, id: Long) {
        val info = getAppInfo(position)
        info?.let {
            appClicked(it)
        }
    }

    private fun appClicked(appInfo: AppInfo) {
        val pm = packageManager
        val moreInfo: ApplicationInfo
        try {
            moreInfo = pm.getApplicationInfo(appInfo.packagePath, 0)
            val apkPath = moreInfo.sourceDir
            if (BuildConfig.PARSER_ONLY) {
                startFullEditActivity(this, apkPath)
            } else if (BuildConfig.LIMIT_NEW_VERSION && upgradedFromOldVersion(this)) {
                startFullEditActivity(this, apkPath)
            } else {
                EditModeView(this, this, apkPath, moreInfo.packageName).showAppEditDialog()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.btn_search) {
            searchApp()
        }
    }

    @SuppressLint("DefaultLocale")
    private fun searchApp() {
        val keyword = keywordEdit?.text.toString()
        val matchedApps: MutableList<AppInfo> = ArrayList()
        for (appInfo in allApps) {
            if (appInfo.appName.lowercase(Locale.getDefault()).contains(keyword.lowercase(Locale.getDefault()))) {
                matchedApps.add(appInfo)
            }
        }
        showAppList(matchedApps)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val info = menuInfo as AdapterContextMenuInfo
        val appInfo = getAppInfo(info.position) ?: return
        menu.setHeaderTitle(appInfo.appName)
        menu.add(0, APP_INFO_ID, 0, R.string.app_info)
        menu.add(0, BACKUP_ID, 0, R.string.backup)
        menu.add(0, LAUNCH_ID, 0, R.string.launch)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterContextMenuInfo
        val position = info.position
        when (item.itemId) {
            APP_INFO_ID -> {
                showAppInfo(position)
                return true
            }
            BACKUP_ID -> {
                backupApp(position)
                return true
            }
            LAUNCH_ID -> {
                launchApp(position)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // App Detail/information
    private fun showAppInfo(position: Int) {
        try {
            val info = getAppInfo(position) ?: return
            val packageName = info.packagePath
            try {
                // Open the specific App Info page:
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (ignored: ActivityNotFoundException) {
                // Open the generic Apps page:
                val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Copy apk of the target app to sd card
    private fun backupApp(position: Int) {
        try {
            val info = getAppInfo(position) ?: return
            val appInfo = packageManager.getApplicationInfo(info.packagePath, 0)
            val appName = info.appName
            val apkPath = appInfo.publicSourceDir
            ProgressDialog(
                this, "", "Working…", false,
                object : ProcessingInterface {
                    private var outPath: String? = null
                    private var succeed = false

                    @Throws(Exception::class)
                    override fun process() {
                        outPath = makeBackupDir(this@UserAppActivity) + appName + ".apk"
                        var input: FileInputStream? = null
                        var out: FileOutputStream? = null
                        try {
                            input = FileInputStream(apkPath)
                            out = FileOutputStream(outPath)
                            IOUtils.copy(input, out)
                            succeed = true
                        } finally {
                            IOUtils.closeQuietly(input)
                            IOUtils.closeQuietly(out)
                        }
                    }

                    override fun afterProcess() {
                        if (succeed) {
                            val format = getString(R.string.apk_saved_tip)
                            val message = String.format(format, outPath)
                            Toast.makeText(this@UserAppActivity, message, Toast.LENGTH_LONG).show()
                        }
                    }
                }, -1
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchApp(position: Int) {
        try {
            val info = getAppInfo(position) ?: return
            if (!openApp(this, info.packagePath)) {
                val message = String.format(getString(R.string.cannot_launch), info.appName)
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Simple edit or full edit clicked
    override fun editModeSelected(mode: Int, filePath: String?) {
        var intent: Intent? = null
        when (mode) {
            EditModeView.SIMPLE_EDIT -> {
                intent = Intent(this, SimpleEditActivity::class.java)
            }
            EditModeView.FULL_EDIT -> {
                if (startFullEditActivity(this, filePath)) {
                    finish()
                }
                return
            }
            EditModeView.COMMON_EDIT -> {
                intent = Intent(this, CommonEditActivity::class.java)
            }
            EditModeView.XML_FILE_EDIT -> {
                intent = Intent(this, AxmlEditActivity::class.java)
            }
            EditModeView.DATA_EDIT -> {
                val prefIntent = Intent(this, PrefOverallActivity::class.java)
                val bundle = Bundle()
                bundle.putString("packagePath", filePath)
                bundle.putBoolean("backup", false)
                prefIntent.putExtras(bundle)
                startActivity(prefIntent)
            }
        }
        if (intent != null) {
            ActivityUtils.attachParam(intent, "apkPath", filePath)
            startActivity(intent)
            finish()
        }
    }

    override fun updateFileList(path: String) {
        // nothing
    }

    private class MyHandler(activity: UserAppActivity) : Handler() {
        private val activityRef: WeakReference<UserAppActivity>
        private var appList: MutableList<AppInfo>? = null
        fun setAppList(appList: MutableList<AppInfo>?) {
            this.appList = appList
        }

        override fun handleMessage(msg: Message) {
            val activity = activityRef.get() ?: return
            val list = appList
            if (list != null && msg.what == 0) {
                activity.allApps.clear()
                activity.allApps.addAll(list)
                activity.showAppList(list)
            }
        }

        init {
            activityRef = WeakReference(activity)
        }
    }

    companion object {
        @JvmStatic
        fun startFullEditActivity(activity: Activity, filePath: String?): Boolean {
            val intent = Intent(activity, ApkInfoExActivity::class.java)
            ActivityUtils.attachParam(intent, "apkPath", filePath)
            val fullDecoding = true
            ActivityUtils.attachBoolParam(intent, "isFullDecoding", fullDecoding)
            activity.startActivity(intent)
            return true
        }

        fun openApp(context: Context, packageName: String?): Boolean {
            val manager = context.packageManager
            return try {
                packageName?.let { pkg ->
                    val intent = manager.getLaunchIntentForPackage(pkg) ?: return false
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                    context.startActivity(intent)
                }
                true
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                false
            }
        }

        private const val APP_INFO_ID = Menu.FIRST + 1
        private const val BACKUP_ID = Menu.FIRST + 2
        private const val LAUNCH_ID = Menu.FIRST + 3
    }
}