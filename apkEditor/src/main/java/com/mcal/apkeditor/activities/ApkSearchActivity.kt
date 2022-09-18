package com.mcal.apkeditor.activities

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.ApkListAdapter
import com.mcal.apkeditor.R
import com.mcal.apkeditor.activities.UserAppActivity.Companion.startFullEditActivity
import com.mcal.apkeditor.se.SimpleEditActivity
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.utilsOld.ActivityUtils
import java.io.File
import java.util.*

class ApkSearchActivity : CustomizedLangActivity(), AdapterView.OnItemClickListener {
    private val apkFileList: MutableList<String> = ArrayList()
    private var keyword: String? = null
    private var searchPath: String? = null
    private var searchingLayout: View? = null
    private var mAdapter: ApkListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apksearch)
        val intent = intent
        keyword = ActivityUtils.getParam(intent, "Keyword")
        searchPath = ActivityUtils.getParam(intent, "Path")
        initView()

        // Start the searching thread
        ApkSearchThread().start()
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

    private fun initView() {
        searchingLayout = findViewById(R.id.searching_layout)
        val apkListView = findViewById<ListView>(R.id.listview_apkfiles)
        val adapter = ApkListAdapter(this)
        mAdapter = adapter
        apkListView.adapter = adapter
        apkListView.onItemClickListener = this
        setTitleText(0)
    }

    // To notify the an apk file is found
    fun foundApkFile(apkPath: String) {
        runOnUiThread {
            val list = apkFileList
            list.add(apkPath)
            setTitleText(list.size)
            mAdapter?.addApkFile(apkPath)
        }
    }

    private fun setTitleText(apkNum: Int) {
        val word = keyword
        var title = String.format(getString(R.string.str_files_found), apkNum, word)
        if ("" == word) { // Not show the last "- ''"
            title = title.substring(0, title.length - 4)
        }
        setupToolbar(R.id.toolbar, title, true)
    }

    // To notify the searching is done
    fun apkSearchDone() {
        runOnUiThread { searchingLayout?.visibility = View.GONE }
    }

    override fun onItemClick(arg0: AdapterView<*>?, arg1: View, position: Int, arg3: Long) {
        val list = apkFileList
        if (position < list.size) {
            val filePath = list[position]
            editModeDialog(filePath)
        }
    }

    private fun editModeDialog(filePath: String) {
        val dialog = MaterialAlertDialogBuilder(this)
        var intent: Intent?
        dialog.setItems(
            arrayOf(
                getString(R.string.full_edit),
                getString(R.string.simple_edit),
                getString(R.string.common_edit),
                getString(R.string.xml_file_edit)
            )
        ) { p112: DialogInterface, p2: Int ->
            when (p2) {
                FileListActivity.SIMPLE_EDIT -> {
                    intent = Intent(this, SimpleEditActivity::class.java)
                    intent?.let { i ->
                        ActivityUtils.attachParam(i, "apkPath", filePath)
                        startActivity(i)
                        finish()
                    }
                    p112.dismiss()
                }
                FileListActivity.FULL_EDIT -> {
                    if (startFullEditActivity(this, filePath)) {
                        finish()
                    }
                    p112.dismiss()
                }
                FileListActivity.COMMON_EDIT -> {
                    intent = Intent(this, CommonEditActivity::class.java)
                    intent?.let { i ->
                        ActivityUtils.attachParam(i, "apkPath", filePath)
                        startActivity(i)
                        finish()
                    }
                    p112.dismiss()
                }
                FileListActivity.XML_FILE_EDIT -> {
                    intent = Intent(this, AxmlEditActivity::class.java)
                    intent?.let { i ->
                        ActivityUtils.attachParam(i, "apkPath", filePath)
                        startActivity(i)
                        finish()
                    }
                    p112.dismiss()
                }
            }
        }
        dialog.create().show()
    }

    internal inner class ApkSearchThread : Thread() {
        override fun run() {
            searchPath?.let { path ->
                searchInFolder(File(path))
            }
            apkSearchDone()
        }

        private fun searchInFolder(curDir: File) {
            keyword?.let { word ->
                val lcKey: String = word.lowercase(Locale.getDefault())
                curDir.listFiles()?.let { files ->
                    val dirList: MutableList<File> = ArrayList()
                    for (f in files) {
                        if (f.isFile) {
                            val fileName = f.name
                            if (fileName.endsWith(".apk")
                                && fileName.lowercase(Locale.getDefault()).contains(lcKey)
                            ) {
                                foundApkFile(f.absolutePath)
                            }
                        } else { // Directory
                            dirList.add(f)
                        }
                    }
                    // Search in all sub directories
                    for (dir in dirList) {
                        searchInFolder(dir)
                    }
                }
            }
        }
    }
}