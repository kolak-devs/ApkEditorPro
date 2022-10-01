package com.mcal.folderlist

import android.content.Context
import android.content.DialogInterface
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.R
import com.mcal.common.utils.FileRecord
import com.mcal.common.utils.InputHelper
import com.mcal.common.utils.deleteAll
import com.mcal.folderlist.util.OpenFiles
import java.io.File
import java.io.IOException

open class FolderListWrapper(
    private val mContext: Context,
    private val mListView: RecyclerView,
    private val mCurPath: String,
    private val mRootPath: String,
    private val mListener: IListEventListener?,
    producer: IListItemProducer
) : FolderListAdapter.FileItemClick {
    var mAdapter: FolderListAdapter? = null
        private set

    private fun init(producer: IListItemProducer) {
        mAdapter = FolderListAdapter(this, mRootPath, mCurPath, producer)
        mListView.adapter = mAdapter
        mListView.layoutManager = LinearLayoutManager(mContext)
        mListView.adapter = mAdapter
    }

    override fun onClick(position: Int) {
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
                if (!listener.fileClicked(filePath)) {
                    OpenFiles.openFile(mContext, filePath)
                }
            }
            val newDir = adapter.getData(null)
            if (newDir != oldDir) {
                listener.dirChanged(newDir)
            }
        }
    }

    override fun onLongClick(position: Int): Boolean {
        val context = mContext
        // The first item is always the parent folder
        if (position == 0) {
            return true
        }
        val dialog = MaterialAlertDialogBuilder(context)
        dialog.setItems(
            arrayOf(
                context.getString(R.string.file_open_as),
                context.getString(R.string.delete),
                context.getString(R.string.rename),
                context.getString(R.string.new_file)
            )
        ) { p112: DialogInterface, p2: Int ->
            when (p2) {
                0 -> {
                    val fileList: MutableList<FileRecord> = ArrayList()
                    val oldDir = mAdapter?.getData(fileList)
                    val rec = fileList[position]
                    if (rec != null) {
                        val filePath = oldDir + "/" + rec.fileName
                        OpenFiles.openFile(context, filePath)
                    }
                    p112.dismiss()
                }
                1 -> {
                    deleteFile(position)
                    p112.dismiss()
                }
                2 -> {
                    showRenameDlg(position)
                    p112.dismiss()
                }
                3 -> {
                    createFile()
                    p112.dismiss()
                }
            }
        }
        dialog.create().show()
        mListener?.itemLongClicked()
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
        val context = mContext
        var ret = false
        val newFile = File("$dirPath/$newName")
        if (newFile.exists()) {
            val tip = context.resources.getString(R.string.file_already_exist)
            val msg = String.format(tip, newName)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } else {
            ret = File("$dirPath/$fileName").renameTo(newFile)
            val strRename = context.resources.getString(R.string.rename)
            val strResult = context.resources.getString(if (ret) R.string.succeed else R.string.failed)
            Toast.makeText(context, "$strRename $strResult", Toast.LENGTH_SHORT).show()
        }
        return ret
    }

    private fun createFile() {
        val context = mContext
        val dirPath = mAdapter?.getData(null)
        val inputDlg = MaterialAlertDialogBuilder(context)
        inputDlg.setTitle(R.string.new_file)
        inputDlg.setMessage(R.string.pls_input_filename)

        // Set an EditText view to get user input
        val input = EditText(context)
        val filter = InputHelper.getFileNameFilter()
        input.filters = arrayOf(filter)
        inputDlg.setView(input)
        inputDlg.setPositiveButton(android.R.string.ok) { _, _ ->
            var name = input.text.toString()
            name = name.trim { it <= ' ' }
            if ("" == name) {
                Toast.makeText(context, R.string.empty_input_tip, Toast.LENGTH_LONG).show()
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
                        errMessage = context.getString(R.string.failed_create_file)
                    }
                } catch (e: IOException) {
                    val fmt = context.getString(R.string.general_error)
                    errMessage = String.format(fmt, e.message)
                }
                if (!succeed) {
                    Toast.makeText(context, errMessage, Toast.LENGTH_LONG).show()
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