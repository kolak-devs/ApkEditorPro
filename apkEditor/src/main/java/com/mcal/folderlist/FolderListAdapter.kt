package com.mcal.folderlist

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.mcal.apkeditor.R
import com.mcal.common.utils.getSubFolder
import com.mcal.common.utils.isParentFolderOf
import com.mcal.common.utilsOld.SDCard
import org.jetbrains.annotations.Contract
import java.io.File

class FolderListAdapter(
    private val ctx: Context,
    private val rootPath: String, // Directly call this function may cause data not synchronized
    private var currentDirectory: String,
    private val producer: IListItemProducer
) : BaseAdapter() {
    private val mFileList: MutableList<FileRecord> = ArrayList()

    fun getItemData(position: Int): FileRecord {
        val fileList = mFileList
        synchronized(fileList) {
            return fileList[position]
        }
    }

    // Directly call this function may cause data not synchronized
    @Contract(" -> new")
    private fun getFileList(): List<FileRecord> {
        synchronized(mFileList) {
            return ArrayList(mFileList)
        }
    }

    // Return directory and sub file records
    fun getData(records: MutableList<FileRecord>?): String? {
        synchronized(mFileList) {
            records?.addAll(mFileList)
            return currentDirectory
        }
    }

    private fun initListData(filePath: String) {
        var path = filePath
        val fileList = mFileList
        val rootDir = rootPath
        synchronized(mFileList) {
            var dir = File(path)
            if (!dir.exists()) {
                path = rootDir
                dir = File(path)
            }
            val subFiles = dir.listFiles()
            if (subFiles != null) {
                fileList.clear()
                for (f in subFiles) {
                    val fr = FileRecord()
                    fr.fileName = f.name
                    fr.isDir = f.isDirectory
                    if (!fr.isDir) {
                        fr.totalSize = f.length()
                    } else {
                        fr.totalSize = -1
                    }
                    fileList.add(fr)
                }
                fileList.sortWith(FilenameComparator())

                // In root directory, will not show parent folder
                if (path != rootDir) {
                    val fr = FileRecord()
                    fr.fileName = ".."
                    fr.isDir = true
                    fr.totalSize = -1
                    fileList.add(0, fr)
                }
                currentDirectory = path
            } else if (isParentFolderOf(path, SDCard.getRootDirectory())) {
                fileList.clear()
                var fr = FileRecord()
                fr.fileName = getSubFolder(path, SDCard.getRootDirectory())
                fr.isDir = true
                fr.totalSize = -1
                fileList.add(fr)

                // In root directory, will not show parent folder
                if (path != rootDir) {
                    fr = FileRecord()
                    fr.fileName = ".."
                    fr.isDir = true
                    fr.totalSize = -1
                    fileList.add(0, fr)
                }
                currentDirectory = path
            }
        }
    }

    override fun getCount(): Int {
        return mFileList.size
    }

    override fun getItem(position: Int): Any {
        return mFileList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("InflateParams", "ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rec = mFileList[position]
        val view = LayoutInflater.from(ctx).inflate(R.layout.item_file, null)
        val viewHolder = ViewHolder()
        viewHolder.icon = view.findViewById(R.id.file_icon)
        viewHolder.filename = view.findViewById(R.id.filename)
        viewHolder.desc1 = view.findViewById(R.id.detail1)
        view.tag = viewHolder
        viewHolder.filename?.text = rec.fileName
        if (rec.fileName == "..") {
            viewHolder.icon?.setImageResource(R.drawable.round_reply_blue_24)
        } else if (rec.isDir) {
            viewHolder.icon?.setImageResource(R.drawable.round_folder_blue_24)
        } else {
            val icon = producer.getFileIcon(currentDirectory, rec)
            if (icon == null) {
                // Use the default icon
                viewHolder.icon?.setImageResource(R.drawable.round_insert_drive_file_24)
            } else {
                viewHolder.icon?.setImageDrawable(icon)
            }
        }
        val detailInfo = producer.getDetail1(currentDirectory, rec)
        if (detailInfo != null) {
            viewHolder.desc1?.text = detailInfo
            viewHolder.desc1?.visibility = View.VISIBLE
        } else {
            viewHolder.desc1?.visibility = View.GONE
        }
        return view
    }

    fun openDirectory(targetPath: String?) {
        // Target path is not correct
        targetPath?.let { path ->
            if (rootPath.startsWith(path) && path != rootPath) {
                return
            }
            initListData(path)
            notifyDataSetChanged()
        }
    }

    fun fileRenamed(dirPath: String?, fileName: String, newName: String?) {
        val fileList = mFileList
        synchronized(mFileList) {
            for (rec in fileList) {
                if (rec.fileName == fileName) {
                    rec.fileName = newName
                    break
                }
            }
        }
        notifyDataSetChanged()
    }

    fun fileDeleted(dirPath: String?, fileName: String) {
        val fileList = mFileList
        synchronized(mFileList) {
            for (i in fileList.indices) {
                val rec = fileList[i]
                if (rec.fileName == fileName) {
                    fileList.removeAt(i)
                    break
                }
            }
        }
        notifyDataSetChanged()
    }

    private class ViewHolder {
        var icon: ImageView? = null
        var filename: TextView? = null
        var desc1: TextView? = null
    }

    init {
        initListData(currentDirectory)
    }
}