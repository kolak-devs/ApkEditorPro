package com.mcal.folderlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mcal.apkeditor.R
import com.mcal.common.utils.*
import org.jetbrains.annotations.Contract
import java.io.File

class FolderListAdapter(
    private val listener: FileItemClick,
    private val rootPath: String, // Directly call this function may cause data not synchronized
    private var currentDirectory: String,
    private val producer: IListItemProducer
) : RecyclerView.Adapter<FolderListAdapter.FileListViewHolder>() {
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
    fun getData(records: MutableList<FileRecord>?): String {
        synchronized(mFileList) {
            records?.addAll(mFileList)
            return currentDirectory
        }
    }

    interface FileItemClick {
        fun onClick(position: Int)
        fun onLongClick(position: Int): Boolean
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
            } else if (isParentFolderOf(path, ScopedStorage.storageDirectory.path)) {
                fileList.clear()
                var fr = FileRecord()
                fr.fileName = getSubFolder(path, ScopedStorage.storageDirectory.path)
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

    fun getItem(position: Int): Any {
        return mFileList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileListViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileListViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FileListViewHolder, position: Int) {
        val rec = mFileList[position]
        holder.filename.text = rec.fileName
        if (rec.fileName == "..") {
            holder.icon.imageLoader(R.drawable.ic_file_up)
        } else if (rec.isDir) {
            holder.icon.imageLoader(R.drawable.ic_folder)
        } else {
            val icon = producer.getFileIcon(currentDirectory, rec)
            if (icon == null) {
                // Use the default icon
                holder.icon.imageLoader(R.drawable.ic_file)
            } else {
                holder.icon.imageLoader(icon)
            }
        }
        val detailInfo = producer.getDetail1(currentDirectory, rec)
        if (detailInfo != null) {
            holder.decription.text = detailInfo
            holder.decription.visibility = View.VISIBLE
        } else {
            holder.decription.visibility = View.GONE
        }
        holder.itemView.setOnClickListener {
            listener.onClick(position)
        }
        holder.itemView.setOnLongClickListener {
            listener.onLongClick(position)
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int {
        return mFileList.size
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

    class FileListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var icon: ImageView = view.findViewById(R.id.file_icon)
        var filename: TextView = view.findViewById(R.id.filename)
        var decription: TextView = view.findViewById(R.id.detail1)
    }

    init {
        initListData(currentDirectory)
    }
}