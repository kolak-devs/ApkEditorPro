package com.mcal.apkeditor.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.LruCache
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.R
import com.mcal.apkeditor.activities.UserAppActivity.Companion.startFullEditActivity
import com.mcal.apkeditor.se.SimpleEditActivity
import com.mcal.apkeditor.ui.fulleditor.FullEditorActivity
import com.mcal.apksigner.ApkSigner
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.data.Preferences
import com.mcal.common.utils.ApkInfoParser
import com.mcal.common.utils.FileRecord
import com.mcal.common.utils.ScopedStorage.externalStoragePath
import com.mcal.common.utils.ScopedStorage.storageDirectory
import com.mcal.common.utilsOld.ActivityUtils
import com.mcal.common.view.ProgressDialog
import com.mcal.editor.TextEditor.getSoraEditor
import com.mcal.folderlist.FolderListWrapper
import com.mcal.folderlist.IListEventListener
import com.mcal.folderlist.IListItemProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.CoroutineContext

class FileListActivity : CustomizedLangActivity(), IListEventListener, IListItemProducer {
    // Image cache
    private val apkIconCache = LruCache<String, ApkInfoParser.AppInfo>(64)
    private var externalStorage: MenuItem? = null
    private var pathTV: String? = null
    private var folderWrapper: FolderListWrapper? = null
    private var parseThread: ApkParseThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listfile)

        if (parseThread == null) {
            parseThread = ApkParseThread()
            parseThread?.run()
        }
        initWithPermChecking()
    }

    public override fun onDestroy() {
        parseThread?.stopParse()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            init()
        }
    }

    private fun initWithPermChecking() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                WRITE_EXTERNAL_STORAGE_REQUEST_CODE
            )
        } else {
            init()
        }
    }

    private fun init() {
        val rootPath = "/"
        val curDir = Preferences.getLastDirectory()
        pathTV = curDir
        setupToolbar(R.id.toolbar, pathTV, true)
        val listView = findViewById<RecyclerView>(R.id.file_list)
        folderWrapper = FolderListWrapper(this, listView, curDir, rootPath, this, this)
        val search = findViewById<EditText>(R.id.search_find)
        val searchBtn = findViewById<ImageButton>(R.id.search_text)
        searchBtn?.setOnClickListener {
            val constraint = search.text.toString()
            if (constraint.isNotEmpty()) {
                val wrapper = folderWrapper
                if (wrapper != null) {
                    wrapper.mAdapter?.getData(null)?.let { currentFolder ->
                        val intent = Intent(this@FileListActivity, ApkSearchActivity::class.java)
                        ActivityUtils.attachParam(intent, "Keyword", constraint)
                        ActivityUtils.attachParam(intent, "Path", currentFolder)
                        this@FileListActivity.startActivity(intent)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_filelist, menu)
        externalStorage = menu.findItem(R.id.external_storage)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.external_storage -> {
                openExtSdCard()
            }
            R.id.internal_storage -> {
                openSdCard()
            }
            R.id.app_files -> {
                openAppFiles()
            }
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        externalStorage?.isVisible = isExistExtSdCard
        return super.onPrepareOptionsMenu(menu)
    }

    private val isExistExtSdCard: Boolean
        get() {
            val path = externalStoragePath
            return if (path != null && path != "") {
                folderWrapper != null
            } else false
        }

    private fun openExtSdCard(): Boolean {
        val path = externalStoragePath
        return if (path != null && path != "") {
            val wrapper = folderWrapper
            if (wrapper != null) {
                wrapper.openDirectory(path)
                true
            } else {
                false
            }
        } else {
            Toast.makeText(
                this, R.string.cannot_find_ext_sdcard,
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    private fun openAppFiles(): Boolean {
        val f = this.filesDir
        val wrapper = folderWrapper
        return if (wrapper != null) {
            wrapper.openDirectory(f.path)
            true
        } else {
            false
        }
    }

    private fun openSdCard() {
        val path = storageDirectory.path
        if (path != "") {
            folderWrapper?.openDirectory(path)
        }
    }

    override fun dirChanged(newDir: String) {
        if (pathTV != null) {
            setupToolbar(R.id.toolbar, newDir, true)
        }
    }

    override fun fileRenamed(dirPath: String, oldName: String, newName: String) = Unit
    override fun fileDeleted(dirPath: String, fileName: String) = Unit
    override fun fileAdded(fileName: String) = Unit
    override fun itemLongClicked() = Unit

    override fun fileClicked(filePath: String): Boolean {
        // Save the directory
        val directory = filePath.substring(0, filePath.lastIndexOf('/'))
        Preferences.setLastDirectory(directory)
        if (filePath.endsWith(".apk")) {
            editModeDialog(filePath)
            return true
        } else if (filePath.endsWith(".so")) {
            val dialog = MaterialAlertDialogBuilder(this)
            dialog.setTitle(R.string.app_translator)
            dialog.setMessage(R.string.open_binary_translator)
            dialog.setPositiveButton(android.R.string.ok) { dialogInterface: DialogInterface, i: Int ->
                val intent = Intent(this@FileListActivity, MainActivity::class.java)
                intent.putExtra("file_path", filePath)
                startActivity(intent)
                dialogInterface.dismiss()
            }
            dialog.setNegativeButton(android.R.string.cancel, null)
            dialog.show()
            return true
        } else if (filePath.endsWith(".java") || filePath.endsWith(".kt") || filePath.endsWith(".xml") ||
            filePath.endsWith(".smali") || filePath.endsWith(".json") || filePath.endsWith(".cpp") ||
            filePath.endsWith(".c") || filePath.endsWith(".h") || filePath.endsWith(".hpp") || filePath.endsWith(
                ".txt"
            )
        ) {
            val intent = getSoraEditor(this, filePath, null, 0, null)
            startActivity(intent)
            return true
        }
        return false
    }

    private fun editModeDialog(filePath: String) {
        val dialog = MaterialAlertDialogBuilder(this)
        var intent: Intent?
        dialog.setItems(
            arrayOf(
                getString(R.string.full_edit),
                getString(R.string.simple_edit),
                getString(R.string.common_edit),
                getString(R.string.xml_file_edit),
                getString(R.string.sign_apk),
                "TEST"
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
                    startFullEditActivity(this, filePath)
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
                    p112.dismiss()
                }
                SIGN_APK -> {
                    sign(filePath, filePath.replace(".apk", "_sign.apk"))
                    p112.dismiss()
                }
                5 -> {
                    intent = Intent(this, FullEditorActivity::class.java)
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

    fun sign(unsignedPath: String, signedPath: String) {
        ProgressDialog(
            this, "Signing", "Please wait...", false,
            object : ProgressDialog.ProcessingInterface {
                @Throws(Exception::class)
                override fun process() {
                    CoroutineScope(Dispatchers.IO).launch {
                        ApkSigner().signApk(unsignedPath, signedPath)
                    }
                }

                override fun afterProcess() {
                    folderWrapper?.let { wrapper ->
                        wrapper.mAdapter?.openDirectory(wrapper.mAdapter?.getData(null))
                    }
                    Toast.makeText(this@FileListActivity, "Apk signed success", Toast.LENGTH_SHORT)
                        .show()
                }
            }, -1
        ).show()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun getFileIcon(
        dirPath: String,
        record: FileRecord?
    ): Drawable? {
        if (record == null) {
            return null
        }
        // APK file
        val name = record.fileName
        if (name != null) {
            if (!record.isDir && name.endsWith(".apk")) {
                val path = "$dirPath/$name"
                val info = apkIconCache[path]
                if (info != null) {
                    return info.icon
                }
                parseThread?.addApk(path)
                return ContextCompat.getDrawable(this, R.drawable.round_android_24)
            }
        }
        return null
    }

    override fun getDetail1(
        dirPath: String,
        record: FileRecord
    ): String? {
        val name = record.fileName
        if (name != null) {
            if (!record.isDir && name.endsWith(".apk")) {
                val path = "$dirPath/$name"
                val info = apkIconCache[path]
                return if (info != null) {
                    info.label
                } else {
                    ""
                }
            }
        }
        return null
    }

    internal inner class ApkParseThread : CoroutineScope {
        private val apkList: MutableList<String> = LinkedList()
        private var bStop = false

        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Main

        fun run() = CoroutineScope(Dispatchers.IO).launch {
            val apkParser = ApkInfoParser()
            while (!bStop) {
                var path: String? = null
                synchronized(apkList) {
                    if (apkList.isEmpty()) {
                        try {
                            (apkList as Object).wait()
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                    if (apkList.isNotEmpty()) {
                        path = apkList.removeAt(0)
                    }
                }
                if (path == null) {
                    continue
                }
                var info: ApkInfoParser.AppInfo? = null
                try {
                    info = apkParser.parse(this@FileListActivity, path)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                if (info != null) {
                    apkIconCache.put(path, info)
                    // Обновление списка в адаптере
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(300)
                        folderWrapper?.mAdapter?.notifyDataSetChanged()
                    }
                }
            }
        }

        fun addApk(path: String) {
            synchronized(apkList) {
                apkList.add(path)
                (apkList as Object).notify()
            }
        }

        fun stopParse() {
            bStop = true
            synchronized(apkList) {
                (apkList as Object).notify()
            }
        }
    }

    companion object {
        const val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1

        const val FULL_EDIT = 0
        const val SIMPLE_EDIT = 1
        const val COMMON_EDIT = 2
        const val XML_FILE_EDIT = 3
        const val SIGN_APK = 4
    }
}
