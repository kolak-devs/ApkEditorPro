package com.mcal.apkeditor.se;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.dialogs.FileCopyDialog;
import com.mcal.apkeditor.dialogs.FileSelectDialog;
import com.mcal.apkeditor.dialogs.FileSelectDialog.IFileSelection;
import com.mcal.apklib.AXMLPrinter;
import com.mcal.common.data.Preferences;
import com.mcal.common.utilsOld.ActivityUtils;
import com.mcal.common.utilsOld.IOUtils;
import com.mcal.common.utilsOld.ImageZoomer;
import com.mcal.common.utilsOld.SDCard;
import com.mcal.common.view.ProgressDialog;
import com.mcal.imageviewlib.ViewZipImageActivity;
import com.mcal.neweditor.TextEditor;

import org.jetbrains.annotations.Contract;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileListAdapter extends BaseAdapter implements
        OnItemClickListener, OnItemLongClickListener, OnClickListener,
        IFileSelection, ProgressDialog.ProcessingInterface {

    private final Activity ctx;
    private final IDirChanged dirChangeIf;
    private final Map<String, List<FileInfo>> dir2Files;
    // Record all the replaces (entry name -> file path)
    private final Map<String, String> fileReplaces = new HashMap<>();
    // Image cache
    private final LruCache<String, Bitmap> imageBitmaps = new LruCache<>(
            32) {
        protected void entryRemoved(boolean evicted, String key,
                                    @NonNull Bitmap oldValue, Bitmap newValue) {
            oldValue.recycle();
        }
    };
    // Help to resolve the image
    private final ZipHelper zipHelper;
    // Use to do the sorting
    Comparator<FileInfo> comparator = (fi1, fi2) -> {
        if (fi1.isDir) {
            if (fi2.isDir) {
                return fi1.filename.compareTo(fi2.filename);
            } else {
                return -1;
            }
        } else {
            if (fi2.isDir) {
                return 1;
            } else {
                return fi1.filename.compareTo(fi2.filename);
            }
        }
    };
    private String curDir;
    private List<FileInfo> curFileList;
    // For AXML editing
    private boolean xmlEditMode = false;
    private ZipFile zfile;
    private ZipImageZoomer zipImageZoomer;

    /* interface implementation for ProcessingDialog */
    private boolean convertSucceed;
    private String clickedEntryPath;
    private String decodedXmlPath;

    public ZipFileListAdapter(Activity ctx, IDirChanged dirChangeIf,
                              ZipHelper zipHelper, boolean xmlEditMode) {
        this(ctx, dirChangeIf, zipHelper);
        this.xmlEditMode = xmlEditMode;
    }

    public ZipFileListAdapter(Activity ctx, IDirChanged dirChangeIf,
                              @NonNull ZipHelper zipHelper) {
        this.ctx = ctx;
        this.curDir = "/";
        this.dirChangeIf = dirChangeIf;
        this.dir2Files = zipHelper.dir2Files;
        this.zipHelper = zipHelper;

        // Sort the root dir
        curFileList = dir2Files.get(curDir);
        if (curFileList != null) {
            curFileList.sort(comparator);
        }

        try {
            this.zfile = new ZipFile(zipHelper.getFilePath());
            this.zipImageZoomer = new ZipImageZoomer(zfile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isImageFile(@NonNull String filename) {
        return filename.endsWith(".png") || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg") || filename.endsWith(".gif");
    }

    @Contract(pure = true)
    public static boolean isXmlFIle(@NonNull String filename) {
        return filename.endsWith(".xml");
    }

    // Extract file from a path, not a entry
    public static void extractFromZip(Activity ctx, String zipFilePath, String entryName) {
        List<FileCopyDialog.CopySource> sources = new ArrayList<>();
        FileCopyDialog.CopySource s = new FileCopyDialog.CopySource();
        s.isInApk = true;
        s.isDir = false;
        s.path = entryName;
        sources.add(s);
        extractFromZip(ctx, zipFilePath, sources);
    }

    private static void extractFromZip(@NonNull final Activity ctx, final String zipFilePath,
                                       final List<FileCopyDialog.CopySource> sources) {
        // Select a target folder to extract
        String dlgTitle = ctx.getString(R.string.select_folder);
        IFileSelection callback = new IFileSelection() {
            private final WeakReference<Activity> ctxRef = new WeakReference<>(ctx);

            @Override
            // filePath is the target directory
            // extraStr is the source file/directory
            public void fileSelectedInDialog(
                    String filePath, String extraStr, boolean openFile) {
                FileCopyDialog d = new FileCopyDialog(ctxRef.get(), zipFilePath, null, null, sources, filePath);
                d.show();
            }

            @Override
            public boolean isInterestedFile(String filename, String extraStr) {
                return true;
            }

            @Nullable
            @Contract(pure = true)
            @Override
            public String getConfirmMessage(String filePath, String extraStr) {
                return null;
            }
        };

        new FileSelectDialog(ctx, callback, null, null,
                dlgTitle, true, false, false, null);
    }

    @Override
    public void process() throws Exception {
        InputStream input;
        FileOutputStream output;

        convertSucceed = false;

        if (zfile != null) {
            // The entry is already replaced
            String replaceFile = fileReplaces.get(clickedEntryPath);
            if (replaceFile != null) {
                input = new FileInputStream(replaceFile);
            } else {
                ZipEntry entry = zfile.getEntry(clickedEntryPath);
                input = zfile.getInputStream(entry);
            }

            decodedXmlPath = SDCard.makeWorkingDir(ctx)
                    + clickedEntryPath.replace('/', '_');
            output = new FileOutputStream(decodedXmlPath);

            AXMLPrinter printer = new AXMLPrinter();
            if (input != null && output != null) {
                convertSucceed = printer.convert(input, output);
            }

            // Clean up
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
    }

    @Override
    public void afterProcess() {
        if (convertSucceed) {
            String apkPath = (zipHelper != null ? zipHelper.getFilePath() : null);
            Intent intent = TextEditor.getSoraEditor(ctx, decodedXmlPath, apkPath, 0, null);
            ctx.startActivityForResult(intent, 0);
        } else {
            String fmt = ctx.getString(R.string.failed_to_parse_xml);
            String message = String.format(fmt, clickedEntryPath);
            Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public int getCount() {
        if (isRootDirectory()) {
            return curFileList.size();
        } else if (curFileList != null) {
            return curFileList.size() + 1;
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        if (isRootDirectory()) {
            return curFileList.get(position);
        } else if (curFileList != null) {
            return curFileList.get(position - 1);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (curFileList == null) {
            return null;
        }

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

        int index = (isRootDirectory() ? position : position - 1);
        if (index >= 0) {
            FileInfo fi = curFileList.get(index);
            viewHolder.filename.setText(fi.filename);

            boolean replaceable;
            // Directory
            if (fi.isDir) {
                viewHolder.icon.setImageResource(R.drawable.round_folder_blue_24);
                replaceable = false;
            }
            // For the binary, not allow to replace (also allow replace now)
            else if (isBinaryFile(fi.filename)) {
                viewHolder.icon.setImageResource(R.drawable.round_insert_drive_file_24);
                replaceable = true;
            }
            // For the image, show icon
            else if (isImageFile(fi.filename)) {
                String entryPath = curDir.substring(1) + fi.filename;
                viewHolder.icon.setImageBitmap(getImageIcon(entryPath));
                replaceable = true;
            }
            // Other files
            else {
                viewHolder.icon.setImageResource(R.drawable.round_insert_drive_file_24);
                replaceable = true;
            }

            // For AXML editing mode, does not show image buttons
            if (replaceable && !xmlEditMode) {
                viewHolder.editMenu.setVisibility(View.VISIBLE);
                viewHolder.editMenu.setId(index);
                viewHolder.editMenu.setOnClickListener(this);
                viewHolder.saveMenu.setVisibility(View.VISIBLE);
                viewHolder.saveMenu.setId(index + curFileList.size());
                viewHolder.saveMenu.setOnClickListener(this);
            } else {
                viewHolder.editMenu.setVisibility(View.GONE);
                viewHolder.saveMenu.setVisibility(View.GONE);
            }
        }
        // Show parent item
        else {
            viewHolder.filename.setText("..");
            viewHolder.icon.setImageResource(R.drawable.round_reply_blue_24);
            viewHolder.editMenu.setVisibility(View.GONE);
            viewHolder.saveMenu.setVisibility(View.GONE);
        }

        // No description
        viewHolder.desc1.setVisibility(View.GONE);

        return convertView;
    }

    private boolean isBinaryFile(@NonNull String filename) {
        // seems arsc has some other usage, so we deleted it from here
        return filename.endsWith(".xml") || filename.endsWith(".dex")
                || filename.endsWith(".MF")
                || filename.endsWith(".SF") || filename.endsWith(".RSA")
                || filename.endsWith(".so");
    }

    private Bitmap getImageIcon(String entryName) {
        // Get thumbnail for the image
        Bitmap bitmap;

        bitmap = imageBitmaps.get(entryName);

        if (bitmap == null) {
            String replaceFile = fileReplaces.get(entryName);
            // Get from APK/ZIP file
            if (replaceFile == null) {
                bitmap = zipImageZoomer.getImageThumbnail(entryName, 32, 32);
            }
            // Get from replace file
            else {
                ImageZoomer zoomer = new ImageZoomer();
                bitmap = zoomer.getImageThumbnail(replaceFile, 32, 32);
            }
            // Save to cache
            if (bitmap != null) {
                imageBitmaps.put(entryName, bitmap);
            }
        }

        return bitmap;
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                            long arg3) {

        int index = -1;
        boolean dirChanged = false;

        if (!isRootDirectory()) {
            // Click on the parent item
            if (position == 0) {
                curDir = getParentPath(curDir);
                this.curFileList = dir2Files.get(curDir);
                dirChanged = true;
            } else {
                index = position - 1;
            }
        } else {
            index = position;
        }

        if (index != -1) {
            FileInfo fi = curFileList.get(index);

            // Open the directory
            if (fi.isDir) {
                this.curDir += fi.filename + "/";
                this.curFileList = dir2Files.get(curDir);
                dirChanged = true;
            }

            // Click on the image file
            else if (isImageFile(fi.filename)) {
                viewImageFile(curDir.substring(1) + fi.filename);
            }
            // To edit AXML file
            else if (xmlEditMode && isXmlFIle(fi.filename)) {
                editAXML(curDir.substring(1) + fi.filename);
            }
        }

        if (dirChanged) {
            // Notify that the dir changed
            dirChangeIf.dirChanged(curDir);

            this.notifyDataSetChanged();
        }
    }

    // To view the image
    private void viewImageFile(String entryPath) {
        String replaceFile = fileReplaces.get(entryPath);
        Intent intent = new Intent(ctx, ViewZipImageActivity.class);
        ActivityUtils.attachParam(intent, "fullScreen", Preferences.isFullScreen());
        if (replaceFile == null) {
            ActivityUtils.attachParam(intent, "zipFilePath", zipHelper.getFilePath());
            ActivityUtils.attachParam(intent, "entryName", entryPath);
        } else {
            ActivityUtils.attachParam(intent, "imageFilePath", replaceFile);
        }
        ctx.startActivity(intent);
    }

    private void editAXML(String entryPath) {
        this.clickedEntryPath = entryPath;
        new ProgressDialog(ctx, "", "Working…", false,
                this, -1).show();
    }

    private boolean isRootDirectory() {
        return "/".equals(curDir);
    }

    @NonNull
    private String getParentPath(@NonNull String path) {
        int pos = path.lastIndexOf('/', path.length() - 2);
        return path.substring(0, pos + 1);
    }

    public String getCurrentDir() {
        return curDir;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                   int position, long arg3) {
        // TODO Auto-generated method stub
        return false;
    }

    public Map<String, String> getReplaces() {
        return this.fileReplaces;
    }

    public void addReplace(String entryName, String filePath) {
        fileReplaces.put(entryName, filePath);
    }

    public void destroy() {
        try {
            if (zfile != null) {
                zfile.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Click on the edit/save image
    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();

        // Edit image
        if (id < curFileList.size()) {
            showReplaceDlg(id);
        }

        // Save image
        else if (id < 2 * curFileList.size()) {
            extractFile(id - curFileList.size());
        }
    }

    private void extractFile(int index) {
        if (index >= curFileList.size()) {
            return;
        }

        FileInfo fi = curFileList.get(index);
        String entryName = curDir.substring(1) + fi.filename;
        String zipPath = zipHelper.getFilePath();

        extractFromZip(ctx, zipPath, entryName);
    }

    // position is the clicked item index
    private void showReplaceDlg(int index) {
        if (index < curFileList.size()) {
            FileInfo fi = curFileList.get(index);
            String entryPath = curDir.substring(1) + fi.filename;
            new FileSelectDialog(
                    ctx, this, null, entryPath, ctx.getString(R.string.select_file_replace));
        }
    }

    // Add an image file replace
    @Override
    public void fileSelectedInDialog(String filePath, String entryPath, boolean openFile) {
        // Record the modification
        this.fileReplaces.put(entryPath, filePath);

        // Remove the old cache
        this.imageBitmaps.remove(entryPath);

        // Update the list view
        this.notifyDataSetChanged();

        // Notify that something modified
        ((SimpleEditActivity) ctx).setModified();
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }

    @Override
    public boolean isInterestedFile(String filename, @NonNull String extraStr) {
        int slashPos = extraStr.lastIndexOf('/');
        int dotPos = extraStr.indexOf('.', slashPos + 1);
        if (dotPos != -1) {
            return filename.endsWith(extraStr.substring(dotPos));
        } else {
            return true;
        }
    }

    static class FileInfo {
        String filename;
        boolean isDir;

        public FileInfo(String name, boolean isDir) {
            this.filename = name;
            this.isDir = isDir;
        }
    }
}