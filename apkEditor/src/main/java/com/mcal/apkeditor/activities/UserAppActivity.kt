package com.mcal.apkeditor.activities

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
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.AppInfo
import com.mcal.apkeditor.R
import com.mcal.apkeditor.adapters.AppListAdapter
import com.mcal.apkeditor.databinding.ActivityApplistBinding
import com.mcal.apkeditor.dialogs.selectFullEditDialog
import com.mcal.appdm.PrefOverallActivity
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.copyFile
import com.mcal.common.view.ProgressDialog
import com.mcal.common.view.ProgressDialog.ProcessingInterface
import java.io.File


class UserAppActivity : CustomizedLangActivity(), AppListAdapter.AppItemClick {
    var mAdapter: AppListAdapter? = null
    var appList = mutableListOf<AppInfo>()
    private var userApps: MenuItem? = null
    private var systemApps: MenuItem? = null

    private lateinit var binding: ActivityApplistBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(id = R.id.toolbar, title = getString(R.string.select_apk_from_app), back = true)

        binding.clearText.setOnClickListener {
            binding.etKeyword.setText("")
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
                    setVisibility(binding.progressBar, View.GONE)
                    val recyclerView = binding.applicationList
                    setVisibility(recyclerView, View.VISIBLE)
                    recyclerView.layoutManager = LinearLayoutManager(context)
                    recyclerView.adapter = adapter
                    binding.etKeyword.addTextChangedListener(object : TextWatcher {
                        override fun onTextChanged(
                            s: CharSequence,
                            start: Int,
                            before: Int,
                            count: Int
                        ) = Unit

                        override fun beforeTextChanged(
                            s: CharSequence,
                            start: Int,
                            count: Int,
                            after: Int
                        ) = Unit

                        override fun afterTextChanged(s: Editable) {
                            setVisibility(binding.clearText, if (s.isEmpty()) View.GONE else View.VISIBLE)
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
            }, -1
        ).show()
    }

    override fun onClick(item: AppInfo) {
        try {
            val moreInfo = packageManager.getApplicationInfo(item.packagePath, 0)
            val apkPath = moreInfo.sourceDir
            editModeDialog(apkPath, moreInfo.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun editModeDialog(filePath: String?, pkg: String?) {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setItems(
            arrayOf(
                getString(R.string.full_edit),
                getString(R.string.edit_data_root)
            )
        ) { p112: DialogInterface, p2: Int ->
            when (p2) {
                FULL_EDIT -> {
                    selectFullEditDialog(this, filePath)
                    p112.dismiss()
                }
                DATA_EDIT -> {
                    val intent = Intent(this, PrefOverallActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("packagePath", pkg)
                    bundle.putBoolean("backup", false)
                    intent.putExtras(bundle)
                    startActivity(intent)
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
        setVisibility(binding.appNotFound, if (mode) View.GONE else View.VISIBLE)
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
                        outPath = ScopedStorage.getBackupsDir().path + File.separator + appName + ".apk"
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
        const val DATA_EDIT = 1

        const val DETAILS = 0
        const val BACKUP = 1
        const val LAUNCH = 2
    }
}