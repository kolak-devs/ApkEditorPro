package com.mcal.apkeditor.se;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.dialogs.FileSelectDialog;
import com.mcal.apkeditor.dialogs.FileSelectDialog.IFileSelection;
import com.mcal.common.utils.ZipImageZoomer;
import com.mcal.common.utils.ActivityHelper;
import com.mcal.common.utilsOld.ImageZoomer;
import com.mcal.common.view.DynamicExpandListView;
import com.mcal.pngeditor.ViewZipImageActivity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

public class ImageListAdapter extends BaseAdapter implements
        OnItemClickListener, OnItemLongClickListener, IFileSelection,
        OnClickListener {

    private final WeakReference<DynamicExpandListView> viewRef;
    private final Activity ctx;

    private final List<String> drawableFileList;
    private final HashMap<String, DrawableEntry> drawableEntries;

    private final ZipHelper zipHelper;
    // All replaces (entry path -> file path)
    private final Map<String, String> replaces = new HashMap<>();
    private final LruCache<String, BitmapRec> imageBitmaps = new LruCache<>(32) {
        protected void entryRemoved(boolean evicted, String key,
                                    @NonNull BitmapRec oldValue, BitmapRec newValue) {
            if (oldValue.showingBitmap != null) {
                oldValue.showingBitmap.recycle();
            }
        }
    };
    private ZipImageZoomer zipImageZoomer;
    private ZipFile zfile;

    public ImageListAdapter(DynamicExpandListView listView, Activity ctx,
                            @NonNull ZipHelper zipHelper) {
        this.viewRef = new WeakReference<>(listView);
        this.ctx = ctx;
        this.zipHelper = zipHelper;

        // Get list information from ZipHelper
        this.drawableFileList = zipHelper.drawableNameList;
        this.drawableEntries = zipHelper.drawableEntries;

        try {
            this.zfile = new ZipFile(zipHelper.getFilePath());
            this.zipImageZoomer = new ZipImageZoomer(zfile);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Cannot close?
        }
    }

    @Override
    protected void finalize() throws Throwable {
        closeZipFile();
        super.finalize();
    }

    // Close the zip file
    private void closeZipFile() {
        if (zfile != null) {
            try {
                zfile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getCount() {
        return drawableEntries.size();
    }

    @Override
    public Object getItem(int position) {
        return drawableEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String entryName = drawableFileList.get(position);
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(R.layout.item_zipfile, null);

            viewHolder = new ViewHolder();
            viewHolder.icon = convertView.findViewById(R.id.file_icon);
            viewHolder.filename = convertView.findViewById(R.id.filename);
            viewHolder.desc1 = convertView.findViewById(R.id.detail1);

            viewHolder.editMenu = convertView.findViewById(R.id.menu_edit);
            viewHolder.saveMenu = convertView.findViewById(R.id.menu_save);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String detailInfo;
        viewHolder.filename.setText(entryName);

        // Set up image click listener
        viewHolder.editMenu.setId(position);
        viewHolder.editMenu.setOnClickListener(this);
        viewHolder.saveMenu.setId(drawableFileList.size() + position);
        viewHolder.saveMenu.setOnClickListener(this);

        // Get thumbnail for the image
        Bitmap bitmap;
        DrawableEntry entry = drawableEntries.get(entryName);

        BitmapRec buf = imageBitmaps.get(entryName);
        if (buf != null) {
            bitmap = buf.showingBitmap;
        } else {
            if (entry.replaceFile == null) {
                bitmap = zipImageZoomer.getImageThumbnail(
                        entry.bestQualifier + "/" + entryName, 32, 32);
            } else {
                ImageZoomer zoomer = new ImageZoomer();
                bitmap = zoomer.getImageThumbnail(entry.replaceFile, 32, 32);
            }
            // Save to cache
            buf = new BitmapRec();
            buf.showingBitmap = bitmap;
            imageBitmaps.put(entryName, buf);
        }
        viewHolder.icon.setImageBitmap(bitmap);
        if (entry != null) {
            detailInfo = entry.getAllPaths();
            viewHolder.desc1.setText(detailInfo);
        }

        return convertView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3) {
        viewImageFile(position);
    }

    @Override
    public boolean onItemLongClick(@NonNull AdapterView<?> parent, View arg1,
                                   final int position, long arg3) {
        parent.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            // Extract
            MenuItem item1 = menu.add(0, Menu.FIRST, 0, R.string.extract);
            item1.setOnMenuItemClickListener(item -> {
                extractFile(position);
                return true;
            });
            // View
            MenuItem item3 = menu.add(0, Menu.FIRST + 1, 0, R.string.view);
            item3.setOnMenuItemClickListener(item -> {
                viewImageFile(position);
                return true;
            });
            // Replace
            MenuItem item2 = menu.add(0, Menu.FIRST + 2, 0,
                    R.string.replace);
            item2.setOnMenuItemClickListener(item -> {
                showReplaceDlg(position);
                return true;
            });
        });
        return false;
    }

    // To view the image
    private void viewImageFile(int position) {
        String filename = drawableFileList.get(position);
        DrawableEntry entry = drawableEntries.get(filename);
        Intent intent = new Intent(ctx, ViewZipImageActivity.class);
        if (entry != null) {
            if (entry.replaceFile == null) {
                ActivityHelper.attachParam(intent, "zipFilePath", zipHelper.getFilePath());
                ActivityHelper.attachParam(intent, "entryName", entry.bestQualifier + "/" + filename);
            } else {
                ActivityHelper.attachParam(intent, "imageFilePath", entry.replaceFile);
            }
        }
        ctx.startActivity(intent);
    }

    //
    private void extractFile(int position) {
        String fileName = drawableFileList.get(position);
        DrawableEntry dEntry = drawableEntries.get(fileName);
        if (dEntry != null) {
            String entryName = dEntry.bestQualifier + "/" + fileName;
            String zipPath = zipHelper.getFilePath();
            ZipFileListAdapter.extractFromZip(ctx, zipPath, entryName);
        }
    }

    // position is the clicked item index
    private void showReplaceDlg(int position) {
        String filename = drawableFileList.get(position);

        new FileSelectDialog(
                ctx, this, null, filename, ctx.getString(R.string.select_file_replace));
    }

    // Add an image file replace
    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        DrawableEntry entry = drawableEntries.get(extraStr);
        entry.replaceFile = filePath;

        // Record the modifications
        for (String q : entry.qualifierList) {
            String path = q + "/" + extraStr;
            this.replaces.put(path, filePath);
        }

        // Remove the old cache
        this.imageBitmaps.remove(extraStr);

        // Update list view
        DynamicExpandListView lv = viewRef.get();
        if (lv != null) {
            lv.dataChanged();
        }

        // Notify that something modified
        ((SimpleEditActivity) ctx).setModified();
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }

    @Override
    public boolean isInterestedFile(String filename, String extraStr) {
        return ZipFileListAdapter.isImageFile(filename);
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        // Click on edit image
        if (id < this.drawableFileList.size()) {
            showReplaceDlg(id);
        }
        // Click on the save image
        else if (id < this.drawableFileList.size() * 2) {
            extractFile(id - drawableFileList.size());
        }
    }

    public Map<String, String> getReplaces() {
        return this.replaces;
    }

    static class BitmapRec {
        Bitmap showingBitmap;
        int originWidth;
        int originHeight;
    }
}
