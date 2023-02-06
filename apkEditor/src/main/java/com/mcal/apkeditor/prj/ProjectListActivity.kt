package com.mcal.apkeditor.prj

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.mcal.apkeditor.R
import com.mcal.apkeditor.activities.ApkInfoActivity
import com.mcal.common.utils.ApkInfoParser
import com.mcal.common.utils.ScopedStorage.getProjects
import com.mcal.common.view.ProgressDialog
import ru.svolf.melissa.swipeback.SwipeBackActivity
import java.io.File
import java.lang.ref.WeakReference

open class ProjectListActivity : SwipeBackActivity(), View.OnClickListener {
    private val handler = MyHandler(this)
    private var adapter: ProjectListAdapter? = null
    private var projectFolder // like "/sdcard/ApkEditor/.projects/"
            : File? = null
    private var projectItems: List<ProjectListAdapter.ItemInfo>? = null
    private var thread: IconParseThread? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_projectlist)
        setupToolbar(R.string.projects)
        initUI()
    }

    private fun setupToolbar(title: Int) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setTitle(title)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
    }

    override fun onClick(view: View) {
        val id = view.id
        if (id == R.id.menu_delete) {
            val i = view.tag as Int
            if (i < projectItems!!.size) {
                val item = projectItems!![i]
                removeProject(item)
            }
        }
    }

    private fun removeProject(item: ProjectListAdapter.ItemInfo) {
        ProgressDialog(
            this, "", "Working…", false,
            ProjectRemover(this, item), -1
        ).show()
    }

    fun updateProjectList() {
        projectItems = listProjects(projectFolder!!.path)
        adapter!!.updateData(projectItems!!)
        adapter!!.notifyDataSetChanged()
    }

    public override fun onDestroy() {
        if (thread != null && thread!!.isAlive) {
            thread!!.stopParse()
        }
        super.onDestroy()
    }

    private fun initUI() {
        val projectList = findViewById<ListView>(R.id.project_list)
        projectFolder = try {
            getProjects()
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            return
        }

        // Set list adapter
        projectItems = listProjects(projectFolder!!.path)
        adapter = ProjectListAdapter(this, projectItems)
        projectList.adapter = adapter
        projectList.onItemClickListener = adapter
        thread = IconParseThread()
        thread!!.start()
    }

    private fun listProjects(projectFolder: String?): List<ProjectListAdapter.ItemInfo> {
        val prjDir = File(projectFolder)
        val items: MutableList<ProjectListAdapter.ItemInfo> = ArrayList()
        do {
            val files = prjDir.listFiles() ?: break
            for (f in files) {
                if (f.isFile) {
                    continue
                }
                val prj = findProjectFile(f.listFiles()) ?: continue
                val info = ApkInfoActivity.loadProject(f.path) ?: continue
                items.add(
                    ProjectListAdapter.ItemInfo(
                        f.name,
                        info.apkPath, info.decodeRootPath, prj.lastModified()
                    )
                )
            }
        } while (false)
        if (!items.isEmpty()) {
            val comparator =
                java.util.Comparator { arg0: ProjectListAdapter.ItemInfo, arg1: ProjectListAdapter.ItemInfo -> if (arg0.lastModified < arg1.lastModified) 1 else -1 }
            items.sortWith(comparator)
        }
        return items
    }

    // Look for info.bin
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Respond to the action bar's Up/Home button
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    internal class MyHandler(activity: ProjectListActivity) : Handler() {
        private val icons: MutableMap<String, Drawable?> = HashMap()
        private val actRef: WeakReference<ProjectListActivity>

        init {
            actRef = WeakReference(activity)
        }

        fun setIcon(apkPath: String, drawable: Drawable?) {
            synchronized(icons) { icons.put(apkPath, drawable) }
            sendEmptyMessage(0)
        }

        override fun handleMessage(msg: Message) {
            if (msg.what == 0) {
                synchronized(icons) { actRef.get()!!.adapter!!.setProjectIcon(icons) }
                actRef.get()!!.adapter!!.notifyDataSetChanged()
            }
        }
    }

    internal inner class IconParseThread : Thread() {
        private var stopFlag = false
        fun stopParse() {
            stopFlag = true
        }

        override fun run() {
            val parser = ApkInfoParser()
            var index = 0
            while (!stopFlag && index < projectItems!!.size) {
                val item = projectItems!![index]
                try {
                    val info = parser.parse(this@ProjectListActivity, item.apkPath)
                    handler.setIcon(item.apkPath, info!!.icon)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                index += 1
            }
        }
    }
}