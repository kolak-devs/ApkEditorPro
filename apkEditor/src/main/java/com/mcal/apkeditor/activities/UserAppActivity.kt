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
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.AppInfo
import com.mcal.apkeditor.R
import com.mcal.apkeditor.adapters.AppListAdapter
import com.mcal.apkeditor.se.SimpleEditActivity
import com.mcal.appdm.PrefOverallActivity
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.utils.copyFile
import com.mcal.common.utils.makeBackupDir
import com.mcal.common.utilsOld.ActivityUtils
import com.mcal.common.view.ProgressDialog
import com.mcal.common.view.ProgressDialog.ProcessingInterface
import java.io.File

class UserAppActivity : CustomizedLangActivity(), AppListAdapter.AppItemClick {
    var mAdapter: AppListAdapter? = null
    var appList = mutableListOf<AppInfo>()
    private var mRecyclerView: RecyclerView? = null
    private var searchTextWatcher: EditText? = null
    private var clearSearchText: ImageButton? = null
    private var progressBar: ProgressBar? = null
    private var textNotFound: TextView? = null
    private var userApps: MenuItem? = null
    private var systemApps: MenuItem? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_applist)
        setupToolbar(R.id.toolbar, getString(R.string.select_apk_from_app), true)

        progressBar = findViewById(R.id.progress_bar)
        mRecyclerView = findViewById(R.id.application_list)
        searchTextWatcher = findViewById(R.id.et_keyword)
        textNotFound = findViewById(R.id.app_not_found)
        clearSearchText = findViewById(R.id.clear_text)
        clearSearchText?.setOnClickListener {
            searchTextWatcher?.setText("")
        }
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

    enum class AppType {
        SYSTEMS, USERS
    }

    var lastValue: String? = null
    private fun reScanAppList(listMode: AppType) {
        ProgressDialog(
            this, "Loading", "Please wait...", false,
            object : ProcessingInterface {
                @Throws(java.lang.Exception::class)
                override fun process() {
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

                override fun afterProcess() {
                    val context = this@UserAppActivity
                    val list = appList.sortedBy { it.appName }
                    val adapter = AppListAdapter(context.packageManager, list, context)
                    mAdapter = adapter
                    progressBar?.visibility = View.GONE
                    mRecyclerView?.let { recyclerView ->
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.layoutManager = LinearLayoutManager(context)
                        recyclerView.adapter = adapter
                        searchTextWatcher?.addTextChangedListener(object : TextWatcher {
                            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
                            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
                            override fun afterTextChanged(s: Editable) {
                                clearSearchText?.visibility = if (s.isEmpty()) View.GONE else View.VISIBLE
                                if (adapter.canStartFilterProcess) {
                                    if (!TextUtils.equals(s, lastValue)) {
                                        val constraint = s.toString()
                                        lastValue = constraint
                                        recyclerView.smoothScrollToPosition(0)
                                        adapter.canStartFilterProcess = false
                                        adapter.filter(constraint)
                                        return
                                    }
                                    return
                                }
                                adapter.newValue = s.toString()
                            }
                        })
                    }
                }
            }, -1
        ).show()
    }

    override fun onClick(item: AppInfo) {
        try {
            val moreInfo = packageManager.getApplicationInfo(item.packagePath, 0)
            val apkPath = moreInfo.sourceDir
            editModeDialog(apkPath)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun editModeDialog(filePath: String?) {
        val dialog = MaterialAlertDialogBuilder(this)
        var intent: Intent?
        dialog.setItems(
            arrayOf(
                getString(R.string.full_edit),
                getString(R.string.simple_edit),
                getString(R.string.common_edit),
                getString(R.string.xml_file_edit),
                getString(R.string.edit_data_root)
            )
        ) { p112: DialogInterface, p2: Int ->
            when (p2) {
                SIMPLE_EDIT -> {
                    intent = Intent(this, SimpleEditActivity::class.java)
                    intent?.let { i ->
                        ActivityUtils.attachParam(i, "apkPath", filePath)
                        startActivity(i)
                        finish()
                    }
                    p112.dismiss()
                }
                FULL_EDIT -> {
                    if (startFullEditActivity(this, filePath)) {
                        finish()
                    }
                    p112.dismiss()
                }
                COMMON_EDIT -> {
                    intent = Intent(this, CommonEditActivity::class.java)
                    intent?.let { i ->
                        ActivityUtils.attachParam(i, "apkPath", filePath)
                        startActivity(i)
                        finish()
                    }
                    p112.dismiss()
                }
                XML_FILE_EDIT -> {
                    intent = Intent(this, AxmlEditActivity::class.java)
                    intent?.let { i ->
                        ActivityUtils.attachParam(i, "apkPath", filePath)
                        startActivity(i)
                        finish()
                    }
                }
                DATA_EDIT -> {
                    val prefIntent = Intent(this, PrefOverallActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("packagePath", filePath)
                    bundle.putBoolean("backup", false)
                    prefIntent.putExtras(bundle)
                    startActivity(prefIntent)
                }
            }
        }
        dialog.create().show()
    }

    override fun onLongClick(position: Int) {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setItems(
            arrayOf(
                getString(R.string.app_info),
                getString(R.string.backup),
                getString(R.string.launch),
            )
        ) { p112: DialogInterface, p2: Int ->
            when (p2) {
                DETAILS -> {
                    showAppInfo(position)
                    p112.dismiss()
                }
                BACKUP -> {
                    backupApp(position)
                    p112.dismiss()
                }
                LAUNCH -> {
                    launchApp(position)
                    p112.dismiss()
                }
            }
        }
        dialog.create().show()
    }

    override fun onFoundApp(mode: Boolean) {
        textNotFound?.visibility = if (mode) View.GONE else View.VISIBLE
    }

    private fun showAppInfo(position: Int) {
        try {
            val info = appList[position]
            val packageName = info.packagePath
            try {
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

    private fun openApp(context: Context, packageName: String?): Boolean {
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

    companion object {
        const val FULL_EDIT = 0
        const val SIMPLE_EDIT = 1
        const val COMMON_EDIT = 2
        const val DATA_EDIT = 3
        const val XML_FILE_EDIT = 4

        const val DETAILS = 0
        const val BACKUP = 1
        const val LAUNCH = 2

        @JvmStatic
        fun startFullEditActivity(activity: Activity, filePath: String?): Boolean {
            val intent = Intent(activity, ApkInfoExActivity::class.java)
            ActivityUtils.attachParam(intent, "apkPath", filePath)
            val fullDecoding = true
            ActivityUtils.attachBoolParam(intent, "isFullDecoding", fullDecoding)
            activity.startActivity(intent)
            return true
        }
    }
}