package com.mcal.apkeditor.activities

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.mcal.apkeditor.AppInfo
import com.mcal.apkeditor.BuildConfig
import com.mcal.apkeditor.R
import com.mcal.apkeditor.activities.MainActivity.Companion.upgradedFromOldVersion
import com.mcal.apkeditor.adapters.AppListAdapter
import com.mcal.apkeditor.dialogs.EditModeView
import com.mcal.apkeditor.dialogs.EditModeView.IEditModeSelected
import com.mcal.apkeditor.se.SimpleEditActivity
import com.mcal.appdm.PrefOverallActivity
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.utils.copyFile
import com.mcal.common.utils.makeBackupDir
import com.mcal.common.utilsOld.ActivityUtils
import com.mcal.common.view.ProgressDialog
import com.mcal.common.view.ProgressDialog.ProcessingInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class UserAppActivity : CustomizedLangActivity(), IEditModeSelected, AppListAdapter.AppItemClick {
    var adapter: AppListAdapter? = null
    var appList = mutableListOf<AppInfo>()
    private var recyclerView: RecyclerView? = null
    private var searchTextWatcher: TextInputEditText? = null
    private var progressBar: ProgressBar? = null
    private var userApps: MenuItem? = null
    private var systemApps: MenuItem? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_applist)
        initFullScreen()
        setupToolbar(R.id.toolbar, getString(R.string.select_apk_from_app), true)

        progressBar = findViewById(R.id.progress_bar)
        recyclerView = findViewById(R.id.application_list)
        searchTextWatcher = findViewById(R.id.et_keyword)
        reScanAppList(AppType.USERS)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_applist, menu)
        userApps = menu.findItem(R.id.user_apps)
        systemApps = menu.findItem(R.id.system_apps)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.user_apps) {
            reScanAppList(AppType.USERS)
            systemApps?.isChecked = false
            userApps?.isChecked = true
        } else if (id == R.id.system_apps) {
            reScanAppList(AppType.SYSTEMS)
            userApps?.isChecked = false
            systemApps?.isChecked = true
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
    }

    public override fun onDestroy() {
        super.onDestroy()
    }

    enum class AppType {
        SYSTEMS, USERS
    }

    private fun reScanAppList(listMode: AppType) {
        ProgressDialog(
            this, "Saving", "Please wait...", false,
            object : ProcessingInterface {
                @Throws(java.lang.Exception::class)
                override fun process() {
                    CoroutineScope(Dispatchers.IO).launch {
                        val pm = packageManager
                        val appInfoList = pm.getInstalledApplications(0)

                        appList.clear()
                        if (listMode == AppType.USERS) {
                            for (ai in appInfoList) {
                                if (ai.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                                    appList.add(AppInfo.create(pm, ai))
                                }
                            }
                        } else if (listMode == AppType.SYSTEMS) {
                            for (ai in appInfoList) {
                                if (ai.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                                    appList.add(AppInfo.create(pm, ai))
                                }
                            }
                        }
                    }
                }

                override fun afterProcess() {
                    adapter = AppListAdapter(this@UserAppActivity, appList, this@UserAppActivity)
                    progressBar?.visibility = View.GONE
                    recyclerView?.visibility = View.VISIBLE
                    recyclerView?.layoutManager = LinearLayoutManager(this@UserAppActivity)
                    recyclerView?.adapter = adapter
                    searchTextWatcher?.addTextChangedListener(object : TextWatcher {
                        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                        }

                        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                        }

                        override fun afterTextChanged(s: Editable) {
                            adapter?.filter(s.toString())
                        }
                    })
                }
            }, -1
        ).show()
    }

    override fun onLongClick(position: Int) {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setItems(
            arrayOf(
                this.getString(R.string.app_info),
                this.getString(R.string.backup),
                this.getString(R.string.launch),
            )
        ) { p112: DialogInterface, p2: Int ->
            when (p2) {
                0 -> {
                    showAppInfo(position)
                    p112.dismiss()
                }
                1 -> {
                    backupApp(position)
                    p112.dismiss()
                }
                2 -> {
                    launchApp(position)
                    p112.dismiss()
                }
            }
        }
        dialog.create().show()
    }

    // App Detail/information
    private fun showAppInfo(position: Int) {
        try {
            val info = appList[position]
            val packageName = info.packagePath
            try {
                // Open the specific App Info page:
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun backupApp(position: Int) {
        try {
            val info = appList[position]
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
                        outPath?.let { path ->
                            copyFile(File(apkPath), File(path))
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
            val info = appList[position]
            if (!openApp(this, info.packagePath)) {
                val message = String.format(getString(R.string.cannot_launch), info.appName)
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
    }

    override fun onClick(item: AppInfo) {
        val pm = packageManager
        val moreInfo: ApplicationInfo
        try {
            moreInfo = pm.getApplicationInfo(item.packagePath, 0)
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
}