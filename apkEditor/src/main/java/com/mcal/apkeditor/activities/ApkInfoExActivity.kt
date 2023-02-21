package com.mcal.apkeditor.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.mcal.apkeditor.R
import com.mcal.common.App.Companion.dp2px
import com.mcal.common.utils.*
import com.mcal.common.utils.ClipboardUtils.copyToClipboard
import com.mcal.common.utils.ScopedStorage.getTmpDir
import com.mcal.common.utils.ScopedStorage.storageDirectory
import com.mcal.common.view.AutoCompleteAdapter
import com.mcal.common.view.AutoCompleteTextView
import java.io.*
import java.util.*
import java.util.zip.ZipFile

class ApkInfoExActivity : ApkInfoActivity() {

    private val clickListener = MenuClickListener()
    private lateinit var menuItemReplace: View
    private lateinit var menuItemDetails: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.mainResources.menuHome.setOnClickListener(clickListener)
        binding.mainResources.menuDone.setOnClickListener(clickListener)
        binding.mainResources.menuSelect.setOnClickListener(clickListener)
        binding.mainResources.menuCreateFileOrFolder.setOnClickListener(clickListener)
        binding.mainResources.imageviewTextCheck.setOnClickListener(clickListener)
        binding.mainResources.imageviewInsensitiveCheck.setOnClickListener(clickListener)

        val param = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        binding.mainResources.resMenuLayout.apply {
            addView(createMenuItem(R.drawable.round_save_24, R.string.extract), param)
            menuItemReplace = createMenuItem(R.drawable.ic_copy, R.string.replace)
            addView(menuItemReplace, param)
            addView(createMenuItem(R.drawable.ic_search, R.string.search), param)
            addView(createMenuItem(R.drawable.ic_delete, R.string.delete), param)
            menuItemDetails = createMenuItem(R.drawable.round_menu_24, R.string.detail)
            addView(menuItemDetails, param)
        }
    }

    // Search file name or file content
    fun reverseSearchOption() {
        searchTextContent = !searchTextContent
        binding.mainResources.imageviewTextCheck.setImageResource(if (searchTextContent) R.drawable.round_feed_24 else R.drawable.round_feed_blue_24)
    }

    fun reverseSearchCaseSensitive() {
        searchResSensitive = !searchResSensitive
        binding.mainResources.imageviewInsensitiveCheck.setImageResource(if (searchResSensitive) R.drawable.round_text_format_blue_24 else R.drawable.round_text_format_24)
    }

    // drawable2 is for dark theme
    @SuppressLint("InflateParams")
    private fun createMenuItem(drawable: Int, title: Int): View {
        return LayoutInflater.from(this).inflate(R.layout.item_res_menu, null).apply {
            findViewById<ImageView>(R.id.menu_icon).apply {
                setImageResource(drawable)
            }
            findViewById<TextView>(R.id.menu_title).apply {
                setText(title)
            }
            id = drawable // borrow the drawable id
            setOnClickListener(clickListener)
        }
    }

    private fun enableMenuItem(view: View, enabled: Boolean) {
        val icon = view.findViewById<ImageView>(R.id.menu_icon)
        val tv = view.findViewById<TextView>(R.id.menu_title)
        if (enabled) {
            icon.drawable.alpha = 255
            tv.isEnabled = true
        } else {
            icon.drawable.alpha = 80
            tv.isEnabled = false
        }
        view.isClickable = enabled
        view.isEnabled = enabled
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

    // Resource selection changed
    override fun selectionChanged(selected: Set<Int>) {
        super.selectionChanged(selected)
        if (selected.size == 1) {
            val fileList: MutableList<FileRecord?> = ArrayList()
            resListAdapter.getData(fileList)
            val position = selected.iterator().next()
            val rec = fileList[position]
            enableMenuItem(menuItemReplace, rec != null && !rec.isDir)
            enableMenuItem(menuItemDetails, true)
        } else {
            enableMenuItem(menuItemReplace, false)
            enableMenuItem(menuItemDetails, false)
        }
    }

    internal inner class MenuClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            when (v.id) {
                R.id.menu_home -> {
                    gotoRootDirectory()
                }
                R.id.menu_done -> {
                    resListAdapter.checkAllItems(false)
                }
                R.id.menu_select -> {
                    selectAllOrNone()
                }
                R.id.menu_create_file_or_folder -> {
                    createFolder(0)
                }
                R.id.imageview_text_check -> {
                    reverseSearchOption()
                }
                R.id.imageview_insensitive_check -> {
                    reverseSearchCaseSensitive()
                }
                R.drawable.round_save_24 -> {
                    saveResourcesTo()
                }
                R.drawable.ic_copy -> {
                    replaceFileOrFolder()
                }
                R.drawable.ic_search -> {
                    inputKeywordAndSearch()
                }
                R.drawable.ic_delete -> {
                    deleteSelectedResources()
                }
                R.drawable.round_menu_24 -> {
                    showResourceInformation()
                }
            }
        }

        private fun inputKeywordAndSearch() {
            val context = this@ApkInfoExActivity
            val materialDialog = MaterialAlertDialogBuilder(context)
            materialDialog.setTitle(R.string.search)
            materialDialog.setMessage(R.string.pls_input_keyword)

            // Set an EditText view to get user input
            val adapter = AutoCompleteAdapter(context, "res_keywords")
            val layout = LinearLayout(context)
            val padding16 = dp2px(16f, context).toInt()
            layout.setPadding(padding16, 0, padding16, 0)
            layout.orientation = LinearLayout.VERTICAL
            layout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val input = AutoCompleteTextView(context)
            input.setAdapter(adapter)
            layout.addView(input, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            val caseInsstCb = CheckBox(context)
            caseInsstCb.setText(R.string.case_insensitive)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 8, 0, 0)
            caseInsstCb.layoutParams = params
            layout.addView(caseInsstCb)
            val filenameCb = CheckBox(context)
            filenameCb.setText(R.string.search_file_names)
            val params2 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params2.setMargins(0, 8, 0, 32)
            filenameCb.layoutParams = params2
            layout.addView(filenameCb)
            materialDialog.setView(layout)
            materialDialog.setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                var keyword = input.text.toString()
                keyword = keyword.trim()
                if ("" == keyword) {
                    Toast.makeText(context, R.string.empty_input_tip, Toast.LENGTH_LONG).show()
                } else {
                    val bSearchName = filenameCb.isChecked
                    val bCaseIsst = caseInsstCb.isChecked
                    doSearchInSelectedItems(keyword, bSearchName, bCaseIsst)
                }
            }
            materialDialog.setNegativeButton(android.R.string.cancel, null)
            materialDialog.show()
        }

        // Keyword already input, now search in selected items
        private fun doSearchInSelectedItems(keyword: String?, bSearchName: Boolean, bCaseIsst: Boolean) {
            val selected = resListAdapter.checkedItems
            if (selected.isEmpty()) {
                return
            }
            val records: List<FileRecord> = ArrayList()
            val baseFolder = resListAdapter.getData(records)
            val filenameList = ArrayList<String?>()
            val positions = ArrayList<Int>(selected.size)
            positions.addAll(selected)
            for (index in positions) {
                filenameList.add(records[index].fileName)
            }

            // Call real search
            searchInResourceFiles(keyword, baseFolder, filenameList, bSearchName, !bCaseIsst)
        }

        private fun showResourceInformation() {
            val selected = resListAdapter.checkedItems
            if (selected.isEmpty()) {
                return
            }
            val position = selected.iterator().next()

            // Check the item is directory or not
            val records: List<FileRecord> = ArrayList()
            val curDir = resListAdapter.getData(records)
            val record = records[position]
            createInfoDialog(curDir, record, position)
        }

        // The detail/more/information dialog
        private fun createInfoDialog(curDir: String, record: FileRecord, position: Int) {
            // Get file name and path
            val fileName = record.fileName
            val filepath = curDir + "/" + record.fileName
            val relativePath = filepath.substring(decodeRootPath.length + 1)

            // Get entry name
            var entryName: String? = null
            if (filepath.startsWith("$decodeRootPath/")) {
                val fileEntry = filepath.substring(decodeRootPath.length + 1)
                if (mFileEntry2ZipEntry != null) {
                    entryName = mFileEntry2ZipEntry[fileEntry]
                }
                if (entryName == null) {
                    entryName = fileEntry
                }
                // Check if the entry exist
                var zipFile: ZipFile? = null
                try {
                    zipFile = ZipFile(apkPath)
                    if (zipFile.getEntry(entryName) == null) {
                        entryName = null
                    }
                } catch (e: Exception) {
                    entryName = null
                    e.printStackTrace()
                } finally {
                    closeQuietly(zipFile)
                }
            }

            // Create dialog view
            val view = LayoutInflater.from(this@ApkInfoExActivity).inflate(R.layout.dialog_resfile_more, null)
            val filenameView = view.findViewById<TextInputEditText>(R.id.filename)
            filenameView.setText(fileName)
            val filePathView = view.findViewById<TextView>(R.id.filepath)
            filePathView.text = filepath
            val fileEntryView = view.findViewById<TextView>(R.id.fileentry)
            fileEntryView.text = entryName ?: getString(R.string.not_available)

            // Extract the original entry (for DEBUG)
            val _entry = entryName
            val extractBtn = view.findViewById<Button>(R.id.btn_extract)
            if (record.isDir) {
                extractBtn.visibility = View.GONE
            } else {
                extractBtn.setOnClickListener {
                    try {
                        ZipHelper.unzipFileTo(apkPath, _entry, storageDirectory.toString() + File.separator + "axml")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // Setup rename button listener
            val renameBtn = view.findViewById<Button>(R.id.btn_rename)
            if (!isFullDecoding && record.isDir) {
                renameBtn.visibility = View.GONE
            }
            renameBtn.setOnClickListener {
                val newName = filenameView.text.toString().trim { it <= ' ' }
                // Empty input
                if (newName == "") {
                    Toast.makeText(this@ApkInfoExActivity, R.string.empty_input_tip, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                // Not changed
                if (newName == record.fileName) {
                    Toast.makeText(this@ApkInfoExActivity, R.string.no_change_detected, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                // If file extension is changed, show tip
                if (!record.isDir && isExtensionChanged(record.fileName!!, newName)) {
                    val dlg = MaterialAlertDialogBuilder(this@ApkInfoExActivity)
                    dlg.setMessage(R.string.extension_changed_tip)
                    dlg.setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int -> doFileRename(curDir, record, _entry, newName, position) }
                    dlg.setNegativeButton(R.string.no, null)
                    dlg.show()
                } else {
                    doFileRename(curDir, record, _entry, newName, position)
                }
            }
            val infoDlg = MaterialAlertDialogBuilder(this@ApkInfoExActivity)
            infoDlg.setTitle(R.string.detail)
            infoDlg.setView(view)
            infoDlg.setNeutralButton(
                R.string.copy_file_path
            ) { _: DialogInterface?, _: Int ->
                val ctx: Context = this@ApkInfoExActivity
                copyToClipboard(ctx, relativePath)
                var msg = ctx
                    .getString(R.string.copied_to_clipboard)
                msg = String.format(msg, relativePath)
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }
            infoDlg.setPositiveButton(android.R.string.ok, null)
            infoDlg.create().show()
        }

        private fun isExtensionChanged(fileName: String, newName: String): Boolean {
            // Origin name has no extension
            var pos = fileName.lastIndexOf('.')
            if (pos == -1) {
                return false
            }
            val extension = fileName.substring(pos)
            pos = newName.lastIndexOf('.')
            if (pos == -1) {
                return true
            }
            val newExt = newName.substring(pos)
            return extension != newExt
        }

        // To secure the rename, we first remove the original one, and then add
        // a new one, but before that, need to make a copy either from decoded
        // file, or from the original apk
        private fun doFileRename(
            curDir: String, record: FileRecord,
            entryName: String?, newName: String, position: Int
        ) {
            // For full decoding, rename is very simple
            if (isFullDecoding) {
                val oldFile = File(curDir + "/" + record.fileName)
                val newFile = File("$curDir/$newName")
                oldFile.renameTo(newFile)
                return
            }
            var tmpFilePath: String? = null

            // Prepare the file content
            var useFileSource = true
            if (record.isInZip) {
                useFileSource = false
            } else {
                record.fileName?.takeIf { it.findExt("jpg|png") }?.let {
                    useFileSource = false
                }
            }
            val zipFile: ZipFile
            val input: InputStream
            try {
                if (useFileSource) {
                    input = FileInputStream(curDir + "/" + record.fileName)
                } else {
                    zipFile = ZipFile(apkPath)
                    val entry = zipFile.getEntry(entryName)
                    input = zipFile.getInputStream(entry)
                }
                tmpFilePath = getTmpDir().toString() + File.separator + getRandomString(6)
                copyFile(input, FileOutputStream(tmpFilePath))
            } catch (e: Exception) {
                Toast.makeText(
                    this@ApkInfoExActivity,
                    R.string.str_rename_failed, Toast.LENGTH_SHORT
                ).show()
                return
            }

            // Delete the old entry
            resListAdapter.deleteFile(curDir, record.fileName, record.isInZip)
            val positions: MutableList<Int> = ArrayList()
            positions.add(position)
            resListAdapter.listItemsDeleted(positions)

            // resListAdapter.addFile(targetPath, filePath);
            renameAddNewfile(curDir, "$curDir/$newName", tmpFilePath)
        }

        private fun renameAddNewfile(dirPath: String, targetPath: String, filePath: String?) {
            var fis: FileInputStream? = null
            try {
                fis = FileInputStream(filePath)
                if (fis != null) {
                    val rec = resListAdapter.addFile(targetPath, fis)
                    if (rec != null) {
                        resListAdapter.listItemAdded(dirPath, rec)
                        Toast.makeText(
                            this@ApkInfoExActivity,
                            R.string.file_renamed, Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@ApkInfoExActivity,
                    R.string.str_rename_failed, Toast.LENGTH_SHORT
                ).show()
            } finally {
                closeQuietly(fis)
            }
        }

        private fun closeQuietly(c: Closeable?) {
            if (c != null) {
                try {
                    c.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        private fun closeQuietly(file: ZipFile?) {
            if (file != null) {
                try {
                    file.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        private fun gotoRootDirectory() {
            resListAdapter.openDirectory(decodeRootPath)
            navigationMgr.gotoDirectory(decodeRootPath)
        }

        private fun deleteSelectedResources() {
            resListAdapter.dumpChangedFiles() // Debug
            val checked = resListAdapter
                .checkedItems
            if (checked.isEmpty()) {
                return
            }
            val selected: MutableList<Int> = ArrayList()
            selected.addAll(checked)
            selected.sort()
            resListAdapter.deleteFile(selected)
        }

        private fun saveResourcesTo() {
            val checked = resListAdapter.checkedItems
            if (checked.isEmpty()) {
                return
            }
            val selected: MutableList<Int> = ArrayList()
            selected.addAll(checked)
            selected.sort()
            this@ApkInfoExActivity.extractFileOrDir(selected)
        }

        private fun selectAllOrNone() {
            val checked = resListAdapter.checkedItems
            var count = resListAdapter.count
            val records: MutableList<FileRecord> = ArrayList(count)
            resListAdapter.getData(records)
            if (".." == records[0].fileName) { // Do not count the
                // parent folder
                count -= 1
            }

            // some are not selected
            resListAdapter.checkAllItems(checked.size != count)
        }

        private fun replaceFileOrFolder() {
            val selected = resListAdapter.checkedItems
            if (selected.isEmpty()) {
                return
            }
            val position = selected.iterator().next()
            replaceFileSAF(position)
        }
    }
}