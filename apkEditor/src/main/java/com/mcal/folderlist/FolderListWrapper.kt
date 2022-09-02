package com.mcal.folderlist

import android.content.Context
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.R
import com.mcal.common.utils.InputHelper
import com.mcal.common.utils.deleteAll
import com.mcal.folderlist.util.OpenFiles
import java.io.File
import java.io.IOException

open class FolderListWrapper(
    private val mContext: Context,
    private val mListView: ListView,
    private val mCurPath: String,
    private val mRootPath: String,
    private val mListener: IListEventListener?,
    producer: IListItemProducer
) : OnItemClickListener, OnItemLongClickListener {
    var mAdapter: FolderListAdapter? = null
        private set

    private fun init(producer: IListItemProducer) {
        mAdapter = FolderListAdapter(mContext, mRootPath, mCurPath, producer)
        mListView.adapter = mAdapter
        mListView.onItemClickListener = this
        mListView.onItemLongClickListener = this
    }

    override fun onItemClick(
        arg0: AdapterView<*>?, arg1: View, position: Int,
        arg3: Long
    ) {
        val fileList: MutableList<FileRecord> = ArrayList()
        val adapter = mAdapter
        val listener = mListener
        if (adapter != null && listener != null) {
            val oldDir = adapter.getData(fileList)
            val rec = fileList[position]
            if (rec.isDir && oldDir != null) {
                val targetPath: String = if (rec.fileName == "..") {
                    val pos = oldDir.lastIndexOf('/')
                    oldDir.substring(0, pos)
                } else {
                    oldDir + "/" + rec.fileName
                }
                adapter.openDirectory(targetPath)
            } else {
                val filePath = oldDir + "/" + rec.fileName
                // When listener not deal with the opening, we will do it
                if (!listener.fileClicked(arg1, filePath)) {
                    OpenFiles.openFile(mContext, filePath)
                }
            }
            val newDir = adapter.getData(null)
            if (newDir != oldDir) {
                listener.dirChanged(newDir)
            }
        }
    }

    override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        // The first item is always the parent folder
        if (position == 0) {
            return true
        }
        parent.setOnCreateContextMenuListener { menu: ContextMenu, v: View?, menuInfo: ContextMenuInfo? ->
            // Open As
            val openAs = menu.add(0, Menu.FIRST, 0, "Open as...")
            openAs.setOnMenuItemClickListener {
                val fileList: MutableList<FileRecord> = ArrayList()
                val oldDir = mAdapter?.getData(fileList)
                val rec = fileList[position]
                if (rec != null) {
                    val filePath = oldDir + "/" + rec.fileName
                    OpenFiles.openFile(mContext, filePath)
                }
                true
            }

            // Delete
            val item1 = menu.add(0, Menu.FIRST, 1, R.string.delete)
            item1.setOnMenuItemClickListener {
                deleteFile(position)
                true
            }

            // Rename
            val item2 = menu.add(0, Menu.FIRST + 2, 0, R.string.rename)
            item2.setOnMenuItemClickListener {
                showRenameDlg(position)
                true
            }

            // New File
            val item3 = menu.add(0, Menu.FIRST + 3, 0, R.string.new_file)
            item3.setOnMenuItemClickListener {
                createFile()
                true
            }
            mListener?.itemLongClicked(menu, v, menuInfo)
        }
        return false
    }

    private fun showRenameDlg(position: Int) {
        val renameDlg = MaterialAlertDialogBuilder(mContext)
        renameDlg.setTitle(R.string.rename)
        renameDlg.setMessage(R.string.pls_input_filename)

        // Set an EditText view to get user input
        val input = EditText(mContext)
        val records: MutableList<FileRecord> = ArrayList()
        val fr = records[position]
        val fileName = fr.fileName
        input.setText(fileName)
        renameDlg.setView(input)
        renameDlg.setPositiveButton(android.R.string.ok) { _, _ ->
            val newName = input.text.toString()
            val adapter = mAdapter
            if (adapter != null) {
                val dirPath = adapter.getData(records)
                if (dirPath != null && fileName != null) {
                    val ret = doRename(dirPath, fileName, newName)
                    if (ret) {
                        adapter.fileRenamed(dirPath, fileName, newName)
                        adapter.fileRenamed(dirPath, fileName, newName)
                    }
                }
            }
        }
        renameDlg.setNegativeButton(android.R.string.cancel, null)
        renameDlg.show()
    }

    private fun doRename(dirPath: String, fileName: String, newName: String): Boolean {
        var ret = false
        val newFile = File("$dirPath/$newName")
        if (newFile.exists()) {
            val tip = mContext.resources.getString(R.string.file_already_exist)
            val msg = String.format(tip, newName)
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show()
        } else {
            ret = File("$dirPath/$fileName").renameTo(newFile)
            val strRename = mContext.resources.getString(R.string.rename)
            val strResult = mContext.resources.getString(if (ret) R.string.succeed else R.string.failed)
            Toast.makeText(mContext, "$strRename $strResult", Toast.LENGTH_SHORT).show()
        }
        return ret
    }

    private fun createFile() {
        val dirPath = mAdapter?.getData(null)
        val inputDlg = MaterialAlertDialogBuilder(mContext)
        inputDlg.setTitle(R.string.new_file)
        inputDlg.setMessage(R.string.pls_input_filename)

        // Set an EditText view to get user input
        val input = EditText(mContext)
        val filter = InputHelper.getFileNameFilter()
        input.filters = arrayOf(filter)
        inputDlg.setView(input)
        inputDlg.setPositiveButton(android.R.string.ok) { _, _ ->
            var name = input.text.toString()
            name = name.trim { it <= ' ' }
            if ("" == name) {
                Toast.makeText(mContext, R.string.empty_input_tip, Toast.LENGTH_LONG).show()
            } else {
                var succeed = false
                var errMessage: String? = null

                // Try to create a new file in current directory
                val dir = File(dirPath)
                val newFile = File(dir, name)
                try {
                    succeed = newFile.createNewFile()
                    if (succeed) {
                        // Update list view
                        mAdapter?.openDirectory(dirPath)
                    } else {
                        errMessage = mContext.getString(R.string.failed_create_file)
                    }
                } catch (e: IOException) {
                    val fmt = mContext.getString(R.string.general_error)
                    errMessage = String.format(fmt, e.message)
                }
                if (!succeed) {
                    Toast.makeText(mContext, errMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
        inputDlg.setNegativeButton(android.R.string.cancel, null)
        inputDlg.show()
    }

    private fun deleteFile(position: Int) {
        val records: MutableList<FileRecord> = ArrayList()
        mAdapter?.let { adapter ->
            val dirPath = adapter.getData(records)
            val fr = records[position]
            val fileName = fr.fileName
            val path = dirPath + "/" + fr.fileName
            val ret = deleteFile(path)
            if (ret && fileName != null) {
                adapter.fileDeleted(dirPath, fileName)
                mListener?.fileDeleted(dirPath, fileName)
            }
        }
    }

    private fun deleteFile(path: String): Boolean {
        var ret = false
        val file = File(path)
        if (file.exists()) {
            if (file.isFile) {
                ret = file.delete()
            } else {
                try {
                    deleteAll(file)
                    ret = true
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return ret
    }

    fun openDirectory(dir: String?) {
        mAdapter?.let { adapter ->
            val oldDir = adapter.getData(null)

            // It may fail, so we need to check after the call
            adapter.openDirectory(dir)
            val newDir = adapter.getData(null)
            if (newDir != oldDir) {
                mListener?.dirChanged(newDir)
            }
        }
    }

    init {
        init(producer)
    }
}