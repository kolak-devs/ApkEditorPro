package com.mcal.apkeditor.adapters;

import static com.mcal.common.utils.FileHelperKt.copyFile;
import static com.mcal.common.utils.PathHelperKt.getSubFolder;
import static com.mcal.common.utils.PathHelperKt.isParentFolderOf;
import static com.mcal.common.utils.StringHelperKt.getRandomString;
import static com.mcal.common.utils.StringHelperKt.join;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mcal.apkeditor.ImageThumbnailInfo;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.ResSelectionChangeListener;
import com.mcal.common.utils.FileRecord;
import com.mcal.common.utils.FilenameComparator;
import com.mcal.common.utils.ImageHelper;
import com.mcal.common.utils.ScopedStorage;
import com.mcal.common.utils.ZipImageHelper;
import com.mcal.common.view.ProgressDialog;

import org.jetbrains.annotations.Contract;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResListAdapter extends BaseAdapter implements
        OnCheckedChangeListener {

    final List<FileRecord> fileList = new ArrayList<>();
    private final WeakReference<Context> ctxRef;
    private final int resourceId;
    private final String apkPath;
    private final String rootPath;
    private final String strResolution;
    private final FilenameFilter filter;
    // use this record to get the real entry inside the apk/zip
    private final Map<String, String> fileEntry2ZipEntry;
    // Record all the checked items
    // When it is not empty, it means in selection mode
    private final Set<Integer> checkedItems = new HashSet<>();
    private final LruCache<String, BitmapInfo> bitmapCache = new LruCache<>(
            64) {
        protected void entryRemoved(boolean evicted, String key,
                                    @NonNull BitmapInfo oldValue, BitmapInfo newValue) {
            if (oldValue.bitmap != null) {
                oldValue.bitmap.recycle();
            }
        }
    };
    // To support in zip exploring for folders like assets
    private final ZipNode rootNode = new ZipNode();
    private WeakReference<ResSelectionChangeListener> listenerRef;
    private String curPath;
    private ZipImageHelper zipImageZoomer;
    private Map<String, String> allFileReplaced = new HashMap<>();
    private Map<String, String> allFileAdded = new HashMap<>();
    // All the deleted entries
    private Set<String> allFileDeleted = new HashSet<>();

    public ResListAdapter(Context ctx, String apkPath, String curPath,
                          String rootPath, FilenameFilter filter) {
        this(ctx, apkPath, curPath, rootPath, null, filter, null);
    }

    public ResListAdapter(Context ctx, String apkPath, String curPath,
                          String rootPath, Map<String, String> fileEntry2ZipEntry,
                          FilenameFilter filter, ResSelectionChangeListener listener) {
        this.ctxRef = new WeakReference<>(ctx);
        if (listener != null) {
            this.listenerRef = new WeakReference<>(listener);
        }

        this.apkPath = apkPath;
        this.rootPath = rootPath;
        this.curPath = curPath;
        this.fileEntry2ZipEntry = fileEntry2ZipEntry;
        this.filter = filter;
        this.strResolution = ctx.getResources().getString(R.string.resolution);

        resourceId = (listener != null ? R.layout.item_file_selectable : R.layout.item_file);

        if (apkPath != null) {
            initZipData(apkPath);
        }

        initListData(curPath);
    }

    // Refresh the list
    public void refresh() {
        openDirectory(curPath);
    }

    private void initZipData(String apkFilePath) {
        try {
            ZipFile zipFile = new ZipFile(apkFilePath);
            this.zipImageZoomer = new ZipImageHelper(zipFile);

            Enumeration<? extends ZipEntry> entryEnum = zipFile.entries();

            ZipEntry entry = null;
            while (entryEnum.hasMoreElements()) {
                entry = entryEnum.nextElement();
                String entryName = entry.getName();
                if (entryName.startsWith("res/") || entryName.startsWith("r/")) {
                    continue;
                }
                if (entryName.startsWith("META-INF/")) {
                    continue;
                }

                String[] paths = entryName.split("/");
                if (paths.length == 1) {
                    if (entryName.equals("AndroidManifest.xml")) {
                        continue;
                    }
                    if (entryName.endsWith(".dex")
                            || entryName.endsWith(".arsc")) {
                        continue;
                    }
                }
                rootNode.addChildByPath(paths, true);
            }

            // zipFile.close();
        } catch (Exception e) {
            e.printStackTrace();
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

    private void initListData(String path) {
        synchronized (fileList) {

            File dir = new File(path);

            // The file is not extracted (get sub files from zip)
            if (!dir.exists()) {
                listSubNodes(path);
            } else {
                listSubFiles(path);
            }
        }
    }

    // List sub nodes in zip file
    @SuppressWarnings("unchecked")
    private void listSubNodes(@NonNull String path) {
        String relativePath = path.substring(rootPath.length() + 1);
        String[] paths = relativePath.split("/");

        ZipNode parent = this.rootNode.findFolderByPath(paths);
        if (parent != null) {
            fileList.clear();

            List<ZipNode> children = parent.getChildren();
            if (children != null) {
                for (ZipNode node : children) {
                    FileRecord fr = new FileRecord();
                    fr.fileName = node.name;
                    fr.isDir = !node.isFile;
                    fr.isInZip = true;
                    fileList.add(fr);
                }
                fileList.sort(new FilenameComparator());
            }

            // This path always not in root dir, so add ".."
            FileRecord fr = new FileRecord();
            fr.fileName = "..";
            fr.isDir = true;
            fileList.add(0, fr);

            curPath = path;
        }
    }

    @SuppressWarnings("unchecked")
    private void listSubFiles(String path) {
        File dir = new File(path);
        boolean isRootDir = path.equals(rootPath);
        File[] subFiles;
        if (isRootDir && filter != null) {
            subFiles = dir.listFiles(filter);
        } else {
            subFiles = dir.listFiles();
        }
        if (subFiles != null) {
            fileList.clear();

            for (File f : subFiles) {
                FileRecord fr = new FileRecord();
                fr.fileName = f.getName();
                fr.isDir = f.isDirectory();
                fileList.add(fr);
            }

            // Don't know why it may throw IllegalArgumentException
            try {
                fileList.sort(new FilenameComparator());
            } catch (Exception ignored) {
            }

            // In root directory, will not show parent folder
            if (!isRootDir) {
                FileRecord fr = new FileRecord();
                fr.fileName = "..";
                fr.isDir = true;
                fileList.add(0, fr);
            } else {
                // Also list root entry in zip file
                List<FileRecord> extraRecords = new ArrayList<>();
                List<ZipNode> children = rootNode.children;
                if (children != null && !children.isEmpty()) {
                    for (ZipNode node : children) {
                        FileRecord fr = new FileRecord();
                        fr.fileName = node.name;
                        fr.isDir = !node.isFile;
                        fr.isInZip = true;
                        extraRecords.add(fr);
                    }
                    extraRecords.sort(new FilenameComparator());
                    fileList.addAll(extraRecords);
                }
            }

            curPath = path;
        }
        // Special case: in the parent path of SD card (like /storage/emulated/0)
        // As on some phones, we cannot access the directory like /storage/emulated
        else if (isParentFolderOf(path, ScopedStorage.getStorageDirectory().getPath())) {
            fileList.clear();
            FileRecord fr = new FileRecord();
            fr.fileName = getSubFolder(path, ScopedStorage.getStorageDirectory().getPath());
            fr.isDir = true;
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

    // Get image thumbnail by file path
    public ImageThumbnailInfo getImageInfo(@NonNull String filepath) {
        String entryName = filepath.substring(rootPath.length() + 1);
        return getImageInfo(entryName, false);
    }

    // Get image thumbnail by entry name
    @NonNull
    @Contract("_, _ -> new")
    private ImageThumbnailInfo getImageInfo(String entryName, boolean bInZip) {
        Bitmap bitmap = null;
        String detailInfo = null;

        // Get bitmap info from replaced file
        BitmapInfo bmpInfo = getModifiedImageInfo(entryName);
        if (bmpInfo != null) {
            bitmap = bmpInfo.bitmap;
            detailInfo = strResolution + ": " + bmpInfo.width + " X "
                    + bmpInfo.height;
        }
        // The file is not replaced and no apk provided
        else if (apkPath == null) {
            ImageHelper zoomer = new ImageHelper();
            bitmap = zoomer.getImageThumbnail(rootPath + "/" + entryName, 32, 32);
            detailInfo = strResolution + ": " + zoomer.getOriginWidth() + " X " + zoomer.getOriginHeight();
        }
        // The file is not replaced (look into file system and apk file)
        else {
            // image (.9.png) is in file
            if (!bInZip && entryName.endsWith(".9.png")) {
                ImageHelper zoomer = new ImageHelper();
                bitmap = zoomer.getImageThumbnail(rootPath + "/" + entryName, 32, 32);
                detailInfo = strResolution + ": " + zoomer.getOriginWidth() + " X " + zoomer.getOriginHeight();
            }
            // Get image from zip
            // Note: image on file system is dummy
            else {
                if (fileEntry2ZipEntry != null) {
                    String t = fileEntry2ZipEntry.get(entryName);
                    if (t != null) {
                        entryName = t;
                    }
                }

                bmpInfo = getImageInfoFromZip(entryName);
                if (bmpInfo != null) {
                    bitmap = bmpInfo.bitmap;
                    detailInfo = strResolution + ": " + bmpInfo.width + " X "
                            + bmpInfo.height;
                }
            }
        }
        return new ImageThumbnailInfo(bitmap, detailInfo);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FileRecord rec = fileList.get(position);
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctxRef.get()).inflate(resourceId,
                    null);

            viewHolder = new ViewHolder();
            viewHolder.icon = convertView
                    .findViewById(R.id.file_icon);
            viewHolder.filename = convertView
                    .findViewById(R.id.filename);
            viewHolder.desc1 = convertView
                    .findViewById(R.id.detail1);
            viewHolder.checkbox = convertView
                    .findViewById(R.id.checkBox);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String detailInfo = null;
        viewHolder.filename.setText(rec.fileName);
        if (rec.fileName.equals("..")) {
            viewHolder.icon.setImageResource(R.drawable.ic_file_up);
        } else if (rec.isDir) {
            viewHolder.icon.setImageResource(R.drawable.ic_folder);
        } else {
            if (rec.fileName.endsWith(".xml")) {
                viewHolder.icon.setImageResource(R.drawable.round_insert_drive_file_yellow_24);
            }
            if (rec.fileName.endsWith(".dex")) {
                viewHolder.icon.setImageResource(R.drawable.round_insert_drive_file_green_24);
            }
            if (isImageFile(rec.fileName)) {
                String entryName = getEntryName(curPath, rec.fileName);
                ImageThumbnailInfo info = getImageInfo(entryName, rec.isInZip);
                if (info.thumbnail == null) {
                    viewHolder.icon.setImageResource(R.drawable.ic_warning);
                } else {
                    viewHolder.icon.setImageBitmap(info.thumbnail);
                }
                detailInfo = info.detailInfo;
            } else if (rec.fileName.endsWith(".xml")) {
                viewHolder.icon.setImageResource(R.drawable.round_insert_drive_file_yellow_24);
            } else if (rec.fileName.endsWith(".dex")) {
                viewHolder.icon.setImageResource(R.drawable.round_insert_drive_file_green_24);
            } else {
                viewHolder.icon.setImageResource(R.drawable.ic_file);
            }
        }

        if (detailInfo != null) {
            viewHolder.desc1.setText(detailInfo);
            viewHolder.desc1.setVisibility(View.VISIBLE);
        } else {
            viewHolder.desc1.setVisibility(View.GONE);
        }

        // checkbox
        if (viewHolder.checkbox != null) {
            if (position == 0 && rec.fileName.equals("..")) {
                viewHolder.checkbox.setVisibility(View.INVISIBLE);
            } else {
                viewHolder.checkbox.setVisibility(View.VISIBLE);
                viewHolder.checkbox.setId(position);
                viewHolder.checkbox.setOnCheckedChangeListener(this);
                viewHolder.checkbox.setChecked(this.checkedItems
                        .contains(position));
            }
        }
        return convertView;
    }

    // Get entry name by current directory and file name
    private String getEntryName(@NonNull String curPath, String fileName) {
        String entryName;
        if (curPath.equals(rootPath)) {
            entryName = fileName;
        } else {
            int position = rootPath.endsWith("/") ? rootPath.length()
                    : (rootPath.length() + 1);
            entryName = curPath.substring(position) + "/" + fileName;
        }
        return entryName;
    }

    // Get the image info from modified records (added or replaced)
    @Nullable
    private BitmapInfo getModifiedImageInfo(String entryName) {
        String path = this.allFileAdded.get(entryName);
        if (path == null) {
            path = this.allFileReplaced.get(entryName);
        }

        if (path != null) {
            ImageHelper zoomer = new ImageHelper();
            Bitmap bitmap = zoomer.getImageThumbnail(path, 32, 32);
            if (bitmap != null) {
                return new BitmapInfo(bitmap, zoomer.getOriginWidth(), zoomer.getOriginHeight());
            }
        }
        return null;
    }

    private BitmapInfo getImageInfoFromZip(String entryName) {
        // Get thumbnail, width and height
        BitmapInfo bmpInfo;

        bmpInfo = bitmapCache.get(entryName);

        if (bmpInfo == null) {
            Bitmap bitmap = zipImageZoomer.getImageThumbnail(entryName, 32, 32);
            if (bitmap != null) {
                int width = zipImageZoomer.getOriginWidth();
                int height = zipImageZoomer.getOriginHeight();
                bmpInfo = new BitmapInfo(bitmap, width, height);
            }
            // Save to cache
            if (bmpInfo != null) {
                bitmapCache.put(entryName, bmpInfo);
            }
        }

        return bmpInfo;
    }

    public boolean isImageFile(@NonNull String fileName) {
        return fileName.endsWith(".png") || fileName.endsWith(".jpg");
    }

    public void openDirectory(String targetPath) {
        // Target path is not correct
        if (rootPath.startsWith(targetPath) && !targetPath.equals(rootPath)) {
            return;
        }
        initListData(targetPath);
        this.notifyDataSetChanged();
    }

    // Removed several list items
    public void listItemsDeleted(List<Integer> positions) {
        Iterator<FileRecord> it = fileList.iterator();
        int index = 0;
        while (it.hasNext()) {
            it.next();
            if (positions.contains(index)) {
                it.remove();
            }
            index += 1;
        }

        // As it is called in selection mode, need to uncheck
        this.checkedItems.clear();

        this.notifyDataSetChanged();

        // Sometimes, it will not trigger onCheckedChanged
        if (listenerRef.get() != null) {
            listenerRef.get().selectionChanged(checkedItems);
        }
    }

    // Add a list item
    public void listItemAdded(String dirPath, FileRecord rec) {
        synchronized (fileList) {
            if (curPath.equals(dirPath)) {
                fileList.add(rec);
                notifyDataSetChanged();
            }
        }
    }

    public void createFile(@NonNull String targetPath, String fileName) {
        final File file = new File(targetPath);
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    listItemAdded(file.getParent(), new FileRecord(fileName, false, false));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(ctxRef.get(), String.format(ctxRef.get().getString(R.string.file_already_exist), fileName), Toast.LENGTH_LONG).show();
        }
    }

    public void addFile(@NonNull String targetPath, String fileName) {
        final File file = new File(targetPath);
        if (file.exists()) {
            listItemAdded(file.getParent(), new FileRecord(fileName, false, false));
        } else {
            Toast.makeText(ctxRef.get(), "Failed", Toast.LENGTH_LONG).show();
        }
    }

    // This function can be called in non-UI thread
    public FileRecord addFile(@NonNull String targetPath, InputStream input)
            throws Exception {

        int position = targetPath.lastIndexOf('/');
        String dirPath = targetPath.substring(0, position);
        String filename = targetPath.substring(position + 1);
        String entryName;
        if (dirPath.equals(rootPath)) { // special case
            entryName = filename;
        } else {
            entryName = targetPath.substring(rootPath.length() + 1);
        }

        // Special case: in the root dir to check allowed or not
        if (dirPath.equals(rootPath) && this.apkPath != null) {
            checkCanPutInRootPath(filename);
        }

        boolean bInZip;
        File dir = new File(dirPath);
        // Copy to a decoded folder
        if (insideResOrSmali(dirPath) || (dir.exists() && !dirPath.equals(rootPath))) {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            bInZip = false;
            if (new File(targetPath).exists()) {
                throwExistException(entryName);
            } else {
                FileOutputStream out = new FileOutputStream(targetPath);
                copyFile(input, out);
                out.close();
            }
        } else { // copy to a folder inside apk (like assets)
            bInZip = true;
            String[] paths = entryName.split("/");
            ZipNode zipNode = rootNode.findNodeByPath(paths);
            if (zipNode != null) {
                throwExistException(entryName);
            } else {
                // Copy to the working path
                targetPath = ScopedStorage.getTmpDir() + File.separator + getRandomString(8);
                FileOutputStream out = new FileOutputStream(targetPath);
                copyFile(input, out);
                out.close();

                rootNode.addChildByPath(paths, true);
            }
        }

        // Record and update the list
        recordFileAdd(entryName, targetPath);

        return new FileRecord(filename, false, bInZip);
    }

    private boolean insideResOrSmali(@NonNull String dirPath) {
        String resDir = rootPath + "/res";
        if (dirPath.equals(resDir)) {
            return true;
        }
        if (dirPath.startsWith(resDir + "/")) {
            return true;
        }

        String smaliDir = rootPath + "/smali";
        if (dirPath.equals(smaliDir)) {
            return true;
        }
        if (dirPath.startsWith(smaliDir + "/")) {
            return true;
        }
        return dirPath.startsWith(smaliDir + "_");
    }

    // Check this filename can put in the root dir of the apk file or not
    private void checkCanPutInRootPath(@NonNull String filename) throws Exception {
        if (filename.equals("META-INF")) {
            throwExistException(filename);
        }
        ZipFile zfile = new ZipFile(this.apkPath);
        ZipEntry entry = zfile.getEntry(filename);
        if (entry != null) {
            zfile.close();
            throwExistException(filename);
        } else {
            zfile.close();
        }
    }

    // Add an empty folder
    public void addFolder(String dirPath, String folderName) {
        final File folder = new File(dirPath, folderName);
        if (!folder.exists()) {
            if (folder.mkdir()) {
                listItemAdded(folder.getParent(), new FileRecord(folderName, true, false));
            }
        } else {
            Toast.makeText(ctxRef.get(), String.format(ctxRef.get().getString(R.string.file_already_exist), folderName), Toast.LENGTH_LONG).show();
        }
    }

    private void throwExistException(String filename) throws Exception {
        throw new Exception(String.format(
                ctxRef.get().getString(R.string.file_already_exist), filename));
    }

    // For editable files, call this function when it is modified
    public void fileModified(String entryName, String newFilePath) {
        this.recordFileReplace(entryName, newFilePath);
    }

    // A file is selected to replace an existing file (not just
    // image, can be image, smali, and others)
    public void replaceFile(@NonNull String decodedPath, String newPath) {
        String entryPath = decodedPath.substring(rootPath.length() + 1);

        // Do replace in file system
        if (new File(decodedPath).exists()) {
            FileInputStream in = null;
            FileOutputStream out = null;
            try {
                // Copy files
                in = new FileInputStream(newPath);
                out = new FileOutputStream(decodedPath);
                copyFile(in, out);

                // Record replace information and show
                recordFileReplace(entryPath, decodedPath);
                Toast.makeText(ctxRef.get(), R.string.file_replaced,
                        Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeQuietly(in);
                closeQuietly(out);
            }
        }
        // Do replace inside the apk
        else {
            try {
                // Copy file to working directory
                String targetPath = ScopedStorage.getTmpDir() + File.separator + getRandomString(8);
                copyFile(newPath, targetPath);

                // Record replacement and show toast
                recordFileReplace(entryPath, targetPath);
                Toast.makeText(ctxRef.get(), R.string.file_replaced,
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.notifyDataSetChanged();
    }

    // Replace the whole folder
    // Copy all the contents in newPath to decodedPath
    // The folder name will keep unchanged
    public void replaceFolder(final String decodedPath, final String newPath) {
        new ProgressDialog((Activity) ctxRef.get(), "", "Working…", false, new ProgressDialog.ProcessingInterface() {
            @Override
            public void process() throws Exception {
                File dstFolder = new File(decodedPath);
                if (dstFolder.exists()) { // copy to decoded path (file system)
                    List<String> deletedFiles = doDeleteDirectory(dstFolder);
                    if (deletedFiles != null) {
                        for (String delPath : deletedFiles) {
                            String entry = delPath
                                    .substring(rootPath.length() + 1);
                            recordFileDelete(entry);
                        }
                    }
                    File srcFolder = new File(newPath);
                    Map<String, String> added = copyAllFiles(srcFolder,
                            dstFolder,
                            decodedPath.substring(rootPath.length() + 1));
                    for (Map.Entry<String, String> entry : added.entrySet()) {
                        recordFileAdd(entry.getKey(), entry.getValue());
                    }
                } else { // copy to apk/zip node
                    String entry = decodedPath.substring(rootPath.length() + 1);
                    List<String> delEntries = rootNode.deleteByPath(entry
                            .split("/"));
                    if (delEntries != null) {
                        for (String e : delEntries) {
                            recordFileDelete(e);
                        }
                    }
                    // Copy to the working directory (not decoded path)
                    String targetFolder = ScopedStorage.getTmpDir() + File.separator + getRandomString(6);
                    Map<String, String> added = copyAllFiles(new File(newPath),
                            new File(targetFolder), entry);
                    // Record and update the zip nodes
                    for (Map.Entry<String, String> e : added.entrySet()) {
                        rootNode.addChildByPath(e.getKey().split("/"), true);
                        recordFileAdd(e.getKey(), e.getValue());
                    }
                }
            }

            @Override
            public void afterProcess() {
            }
        }, R.string.folder_replaced).show();
    }

    // Copy all files from srcFolder to dstFolder
    // Return all the added entries
    // Note: it will NOT record the added entries
    @NonNull
    private Map<String, String> copyAllFiles(File srcFolder, @NonNull File dstFolder,
                                             String entryName) throws IOException {
        if (!dstFolder.exists()) {
            dstFolder.mkdirs();
        }

        Map<String, String> addedEntries = new HashMap<>();

        File[] files = srcFolder.listFiles();
        if (files != null)
            for (File f : files) {
                String name = f.getName();
                if (f.isFile()) { // Copy a single file
                    File dstFile = new File(dstFolder, f.getName());
                    copyFile(f, dstFile);
                    String filepath = dstFile.getPath();
                    addedEntries.put(entryName + "/" + name, filepath);
                } else { // Copy the sub folder
                    Map<String, String> added = copyAllFiles(f, new File(dstFolder,
                            f.getName()), entryName + "/" + name);
                    addedEntries.putAll(added);
                }
            }

        return addedEntries;
    }

    public void dumpChangedFiles() {
        // Log.d("DEBUG", "Added Entry: ");
        // for (Map.Entry<String, String> entry : allFileAdded.entrySet()) {
        // Log.d("DEBUG", "\t" + entry.getKey() + " --> " + entry.getValue());
        // }
        //
        // Log.d("DEBUG", "Replaced Entry: ");
        // for (Map.Entry<String, String> entry : allFileReplaced.entrySet()) {
        // Log.d("DEBUG", "\t" + entry.getKey() + " --> " + entry.getValue());
        // }
        //
        // Log.d("DEBUG", "Deleted Entry: ");
        // for (String entry : allFileDeleted) {
        // Log.d("DEBUG", "\t" + entry);
        // }
    }

    private void recordFileAdd(String entryName, String filepath) {
        if (this.allFileDeleted.contains(entryName)) {
            this.allFileDeleted.remove(entryName);
            this.allFileReplaced.put(entryName, filepath);
        } else if (this.allFileReplaced.containsKey(entryName)) {
            Log.d("DEBUG", "error: " + entryName + " already exist?");
        } else {
            this.allFileAdded.put(entryName, filepath);
        }
    }

    private void recordFileDelete(String entryName) {
        // The file is new added
        if (this.allFileAdded.containsKey(entryName)) {
            this.allFileAdded.remove(entryName);
        }
        // The file is modified
        else if (this.allFileReplaced.containsKey(entryName)) {
            this.allFileReplaced.remove(entryName);
            this.allFileDeleted.add(entryName);
        }
        // no operation before
        else {
            this.allFileDeleted.add(entryName);
        }
    }

    private void recordFileReplace(String entryName, String filepath) {
        if (this.allFileAdded.containsKey(entryName)) {
            this.allFileAdded.put(entryName, filepath);
        } else if (this.allFileDeleted.contains(entryName)) {
            Log.d("DEBUG", "error: " + entryName + " is already deleted?");
        } else {
            this.allFileReplaced.put(entryName, filepath);
        }
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Recursively delete a directory
    // Return all deleted files
    @NonNull
    private List<String> doDeleteDirectory(File dir) {
        List<String> deletedFiles = new ArrayList<>();
        File[] subFiles = dir.listFiles();
        if (subFiles != null) {
            for (File subF : subFiles) {
                if (subF.isFile()) {
                    subF.delete();
                    deletedFiles.add(subF.getPath());
                } else {
                    deletedFiles.addAll(doDeleteDirectory(subF));
                }
            }
        }
        dir.delete();
        return deletedFiles;
    }

    // Delete a file or directory in resource directory
    // Return all deleted files
    @Nullable
    private List<String> doDeleteFileOrDir(String path) {
        File file = new File(path);

        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
                List<String> deletedFiles = new ArrayList<>();
                deletedFiles.add(path);
                return deletedFiles;
            } else {
                return doDeleteDirectory(file);
            }
        }

        return null;
    }

    // Delete a file or directory in a directory
    // The item to be deleted can in file system or zip file
    public void deleteFile(@NonNull List<Integer> positions) {
        List<FileRecord> records = new ArrayList<>();
        String dirPath = getData(records);

        List<Integer> deletedPositions = new ArrayList<>();
        for (int position : positions) {
            FileRecord fileRec = records.get(position);
            if (fileRec == null) {
                continue;
            }
            // Do NOT check the return value
            // if (deleteFile(dirPath, fileRec)) {
            // deletedPositions.add(position);
            // }
            deleteFile(dirPath, fileRec);
            deletedPositions.add(position);
        }

        dumpChangedFiles();

        listItemsDeleted(deletedPositions);
    }

    private boolean deleteFile(String dirPath, @NonNull FileRecord fileRec) {
        return deleteFile(dirPath, fileRec.fileName, fileRec.isInZip);
    }

    // Can be called in non-UI thread
    public boolean deleteFile(String dirPath, String fileName, boolean bInZip) {
        boolean ret = false;
        List<String> deletedEntries = null;

        if (bInZip) {
            String entryPath;
            if (dirPath.equals(rootPath)) {
                entryPath = fileName;
            } else {
                entryPath = dirPath.substring(rootPath.length() + 1) + "/"
                        + fileName;
            }
            deletedEntries = rootNode.deleteByPath(entryPath.split("/"));
            ret = true;
        } else {
            String path = dirPath + "/" + fileName;
            List<String> deletedFiles = doDeleteFileOrDir(path);
            if (deletedFiles != null) {
                deletedEntries = new ArrayList<>();
                for (String delPath : deletedFiles) {
                    deletedEntries
                            .add(delPath.substring(rootPath.length() + 1));
                }
            }
            ret = (deletedFiles != null);
        }

        // Record deleted entries
        if (deletedEntries != null) {
            for (String delEntry : deletedEntries) {
                recordFileDelete(delEntry);
            }
        }

        return ret;
    }

    // Get the replacing file path by entry name
    // Also look into the added entry
    public String getReplacedFilePath(String entryName) {
        String filepath = allFileReplaced.get(entryName);
        if (filepath == null) {
            filepath = allFileAdded.get(entryName);
        }

        return filepath;
    }

    public Map<String, String> getAddedFiles() {
        return this.allFileAdded;
    }

    public Map<String, String> getReplacedFiles() {
        return this.allFileReplaced;
    }

    public Set<String> getDeletedFiles() {
        return this.allFileDeleted;
    }

    // Recover the previous modification
    public void setModification(Map<String, String> res_addedFiles,
                                Set<String> res_deletedFiles, Map<String, String> res_replacedFiles) {
        if (res_addedFiles != null) {
            this.allFileAdded = res_addedFiles;
        }
        if (res_deletedFiles != null) {
            this.allFileDeleted = res_deletedFiles;
        }
        if (res_replacedFiles != null) {
            this.allFileReplaced = res_replacedFiles;
        }
    }

    // Checkbox selection changed
    @Override
    public void onCheckedChanged(@NonNull CompoundButton view, boolean isChecked) {
        // id is the position
        int id = view.getId();
        if (isChecked) {
            this.checkedItems.add(id);
        } else {
            this.checkedItems.remove(id);
        }

        if (this.listenerRef != null) {
            listenerRef.get().selectionChanged(checkedItems);
        }
    }

    public Set<Integer> getCheckedItems() {
        return this.checkedItems;
    }

    public void reverseCheckStatus(int position) {
        // Omit the first parent item
        if (position == 0) {
            if (!fileList.isEmpty() && "..".equals(fileList.get(0).fileName)) {
                return;
            }
        }

        if (this.checkedItems.contains(position)) {
            this.checkedItems.remove(position);
        } else {
            this.checkedItems.add(position);
        }
        this.notifyDataSetChanged();
    }

    // Select all the items, or un-select all the items
    public void checkAllItems(boolean checkAll) {
        if (checkAll) {
            FileRecord rec = fileList.get(0);
            if (!rec.fileName.equals("..")) { // Not for parent folder
                checkedItems.add(0);
            }
            for (int i = 1; i < fileList.size(); ++i) {
                checkedItems.add(i);
            }
        } else {
            checkedItems.clear();
            if (listenerRef.get() != null) {
                listenerRef.get().selectionChanged(checkedItems);
            }
        }
        notifyDataSetChanged();
    }

    // Check the folder exist or not
    // Note: path is the full path like /data/0/com.xx/...
    public boolean isFolderExist(String path) {
        File f = new File(path);
        if (f.exists()) {
            return true;
        }

        // Special root path
        if (this.rootPath.equals(path)) {
            return true;
        }

        // Exist in the node records
        String relativePath = path.substring(rootPath.length() + 1);
        ZipNode node = rootNode.findFolderByPath(relativePath.split("/"));
        return node != null;
    }

    public boolean isInRootDir() {
        return this.curPath.equals(this.rootPath);
    }

    public void gotoRootDir() {
        openDirectory(this.rootPath);
    }

    // Rename the file paths as the decoded folder rename
    public void renamePathAsFolderRename(String fromPath, String toDir) {
        if (this.allFileAdded != null) {
            for (Map.Entry<String, String> entry : this.allFileAdded.entrySet()) {
                if (entry.getValue().startsWith(fromPath)) {
                    entry.setValue(toDir + File.separator + entry.getValue().substring(fromPath.length()));
                }
            }
        }
        if (this.allFileReplaced != null) {
            for (Map.Entry<String, String> entry : this.allFileReplaced.entrySet()) {
                if (entry.getValue().startsWith(fromPath)) {
                    entry.setValue(toDir + File.separator + entry.getValue().substring(fromPath.length()));
                }
            }
        }
    }

    // Image cache
    static class BitmapInfo {
        Bitmap bitmap;
        int width;
        int height;

        public BitmapInfo(Bitmap bitmap2, int width2, int height2) {
            this.bitmap = bitmap2;
            this.width = width2;
            this.height = height2;
        }
    }

    // To emulate the folder tree in zip file
    public static class ZipNode {
        List<ZipNode> children;
        String name;
        boolean isFile;

        public ZipNode() {
            name = "";
            isFile = false;
        }

        public ZipNode(String name, boolean isFile) {
            this.name = name;
            this.isFile = isFile;
        }

        public void addChildByPath(@NonNull String[] paths, boolean isFile) {
            ZipNode parentNode = this;
            for (int i = 0; i < paths.length - 1; i++) {
                ZipNode childNode = parentNode.findFolder(paths[i], true);
                if (childNode == null) {
                    childNode = new ZipNode(paths[i], false);
                    parentNode.addChild(childNode);
                }
                parentNode = childNode;
            }
            // The last one can be file or directory
            ZipNode leaf = new ZipNode(paths[paths.length - 1], isFile);
            parentNode.addChild(leaf);
        }

        public ZipNode findFolderByPath(@NonNull String[] paths) {
            ZipNode parentNode = this;
            for (int i = 0; i < paths.length; i++) {
                ZipNode childNode = parentNode.findFolder(paths[i], false);
                if (childNode == null) {
                    return null;
                }
                parentNode = childNode;
            }
            return parentNode;
        }

        public ZipNode findNodeByPath(@NonNull String[] paths) {
            ZipNode parentNode = this;
            for (int i = 0; i < paths.length - 1; i++) {
                ZipNode childNode = parentNode.findFolder(paths[i], false);
                if (childNode == null) {
                    return null;
                }
                parentNode = childNode;
            }
            return parentNode.findChildByName(paths[paths.length - 1]);
        }

        private void addChild(ZipNode childNode) {
            if (children == null) {
                children = new ArrayList<>();
            }
            children.add(childNode);
        }

        @Nullable
        private ZipNode findFolder(String name, boolean bCreate) {
            if (children == null) {
                if (bCreate) {
                    children = new ArrayList<>();
                } else {
                    return null;
                }
            }
            for (ZipNode node : children) {
                if (!node.isFile && name.equals(node.name)) {
                    return node;
                }
            }
            return null;
        }

        // Find a child node by name
        @Nullable
        @Contract(pure = true)
        private ZipNode findChildByName(String name) {
            if (children != null) {
                for (ZipNode node : children) {
                    if (name.equals(node.name)) {
                        return node;
                    }
                }
            }
            return null;
        }

        public List<ZipNode> getChildren() {
            return children;
        }

        // Delete the node by path
        // Return all the deleted file entries
        public List<String> deleteByPath(@NonNull String[] paths) {
            ZipNode parentNode = null;
            if (paths.length > 1) {
                String[] newPaths = new String[paths.length - 1];
                for (int i = 0; i < paths.length - 1; i++) {
                    newPaths[i] = paths[i];
                }
                parentNode = findFolderByPath(newPaths);
            } else {
                parentNode = this;
            }
            if (parentNode != null) {
                ZipNode target = parentNode.findChildByName(paths[paths.length - 1]);
                if (target != null) {
                    if (target.isFile) {
                        parentNode.deleteChild(target);
                        List<String> deletedEntries = new ArrayList<>();
                        String delPath = join("/", paths);
                        deletedEntries.add(delPath);
                        return deletedEntries;
                    } else {
                        List<String> deletedEntries = target.enumFiles();
                        if (deletedEntries != null) {
                            String delRootPath = join("/", paths);
                            for (int i = 0; i < deletedEntries.size(); i++) {
                                deletedEntries.set(i, delRootPath + "/" + deletedEntries.get(i));
                            }
                            parentNode.deleteChild(target);
                            return deletedEntries;
                        }
                        parentNode.deleteChild(target);
                    }
                }
            }
            return null;
        }

        // Delete a child
        private boolean deleteChild(ZipNode _child) {
            if (children != null) {
                return children.remove(_child);
            }
            return false;
        }

        // Enum all the sub files recursively
        @Nullable
        private List<String> enumFiles() {
            if (children != null) {
                List<String> entries = new ArrayList<>();
                for (ZipNode child : children) {
                    if (child.isFile) {
                        entries.add(child.name);
                    } else {
                        List<String> subEntries = child.enumFiles();
                        if (subEntries != null) {
                            for (String subEntry : subEntries) {
                                entries.add(child.name + "/" + subEntry);
                            }
                        }
                    }
                }
                return entries;
            }

            return null;
        }
    }

    private static class ViewHolder {
        ImageView icon;
        TextView filename;
        TextView desc1;
        CheckBox checkbox;
    }
}
