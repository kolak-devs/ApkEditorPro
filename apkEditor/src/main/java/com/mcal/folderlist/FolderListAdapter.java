package com.mcal.folderlist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.R;
import com.mcal.common.utilsOld.PathUtils;
import com.mcal.common.utilsOld.SDCard;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FolderListAdapter extends BaseAdapter {
    final List<FileRecord> fileList = new ArrayList<>();
    private final Context ctx;
    private final String rootPath;
    private String curPath;
    private final IListItemProducer producer;

    public FolderListAdapter(Context ctx, String rootPath, String curPath, IListItemProducer producer) {
        this.ctx = ctx;
        this.rootPath = rootPath;
        this.curPath = curPath;
        this.producer = producer;

        initListData(curPath);
    }

    public FileRecord getItemData(int position) {
        synchronized (fileList) {
            return fileList.get(position);
        }
    }

    // Directly call this function may cause data not synchronized
    @SuppressWarnings("unused")
    private String getCurrentDirectory() {
        return curPath;
    }

    // Directly call this function may cause data not synchronized
    @NonNull
    @Contract(" -> new")
    @SuppressWarnings("unused")
    private List<FileRecord> getFileList() {
        synchronized (fileList) {
            return new ArrayList<>(fileList);
        }
    }

    // Return directory and sub file records
    public String getData(List<FileRecord> records) {
        synchronized (fileList) {
            if (records != null) {
                records.addAll(fileList);
            }
            return curPath;
        }
    }

    @SuppressWarnings("unchecked")
    private void initListData(String path) {
        synchronized (fileList) {
            File dir = new File(path);
            if (!dir.exists()) {
                path = this.rootPath;
                dir = new File(path);
            }
            File[] subFiles = dir.listFiles();
            if (subFiles != null) {
                fileList.clear();

                for (File f : subFiles) {
                    FileRecord fr = new FileRecord();
                    fr.fileName = f.getName();
                    fr.isDir = f.isDirectory();
                    if (!fr.isDir) {
                        fr.totalSize = f.length();
                    } else {
                        fr.totalSize = -1;
                    }
                    fileList.add(fr);
                }

                fileList.sort(new FilenameComparator());

                // In root directory, will not show parent folder
                if (!path.equals(rootPath)) {
                    FileRecord fr = new FileRecord();
                    fr.fileName = "..";
                    fr.isDir = true;
                    fr.totalSize = -1;
                    fileList.add(0, fr);
                }

                curPath = path;
            }
            // Special case: in the parent path of SD card (like /storage/emulated/0)
            // As on some phones, we cannot access the directory like /storage/emulated
            else if (PathUtils.isParentFolderOf(path, SDCard.getRootDirectory())) {
                fileList.clear();

                SDCard.getRootDirectory();
                FileRecord fr = new FileRecord();
                fr.fileName = PathUtils.getSubFolder(path, SDCard.getRootDirectory());
                fr.isDir = true;
                fr.totalSize = -1;
                fileList.add(fr);

                // In root directory, will not show parent folder
                if (!path.equals(rootPath)) {
                    fr = new FileRecord();
                    fr.fileName = "..";
                    fr.isDir = true;
                    fr.totalSize = -1;
                    fileList.add(0, fr);
                }

                curPath = path;
            }
        }
    }

    @Override
    public int getCount() {
        return fileList.size();
    }

    @Override
    public Object getItem(int position) {
        return fileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FileRecord rec = fileList.get(position);
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(R.layout.item_file, null);

            viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) convertView
                    .findViewById(R.id.file_icon);
            viewHolder.filename = (TextView) convertView
                    .findViewById(R.id.filename);
            viewHolder.desc1 = (TextView) convertView
                    .findViewById(R.id.detail1);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.filename.setText(rec.fileName);
        if (rec.fileName.equals("..")) {
            viewHolder.icon.setImageResource(R.drawable.round_reply_blue_24);
        } else if (rec.isDir) {
            viewHolder.icon.setImageResource(R.drawable.round_folder_blue_24);
        } else {
            Drawable icon = producer.getFileIcon(curPath, rec);
            if (icon == null) {
                // Use the default icon
                viewHolder.icon.setImageResource(R.drawable.round_insert_drive_file_24);
            } else {
                viewHolder.icon.setImageDrawable(icon);
            }
        }

        String detailInfo = producer.getDetail1(curPath, rec);
        if (detailInfo != null) {
            viewHolder.desc1.setText(detailInfo);
            viewHolder.desc1.setVisibility(View.VISIBLE);
        } else {
            viewHolder.desc1.setVisibility(View.GONE);
        }

        return convertView;
    }

    public void openDirectory(String targetPath) {
        // Target path is not correct
        if (rootPath.startsWith(targetPath) && !targetPath.equals(rootPath)) {
            return;
        }
        initListData(targetPath);
        this.notifyDataSetChanged();
    }

    public void fileRenamed(String dirPath, String fileName, String newName) {
        synchronized (fileList) {
            for (FileRecord rec : fileList) {
                if (rec.fileName.equals(fileName)) {
                    rec.fileName = newName;
                    break;
                }
            }
        }
        this.notifyDataSetChanged();
    }

    public void fileDeleted(String dirPath, String fileName) {
        synchronized (fileList) {
            for (int i = 0; i < fileList.size(); i++) {
                FileRecord rec = fileList.get(i);
                if (rec.fileName.equals(fileName)) {
                    fileList.remove(i);
                    break;
                }
            }
        }
        this.notifyDataSetChanged();
    }

    public void fileAdded(String fileName) {

    }

    private static class ViewHolder {
        ImageView icon;
        TextView filename;
        TextView desc1;
    }
}
