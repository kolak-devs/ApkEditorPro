package com.mcal.apkeditor.dialogs;

import static com.mcal.common.utils.FileHelperKt.copyFile;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.R;
import com.mcal.common.data.Constants;
import com.mcal.common.data.LegacyPreferences;
import com.mcal.common.utils.ScopedStorage;
import com.mcal.common.utils.ZipHelper;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Used for extract function
// It can copy from srcPath to dstPath
public class FileCopyDialog {
    // Source and target
    private final List<CopySource> mCopySources;
    private final String mApkPath;
    private final String mTargetFolder; // In general, not end with "/"

    // Used to get entry name from file path
    private final String mDecodeRootPath;

    // Get real entry in apk by entry name
    private final Map<String, String> mEntryMapping;

    private final MyHandler handler;

    // File will automatically renamed(0) or overwrite
    private int fileRenameOption;

    // The real target path of the copied file/dir
    private String savedFilePath;
    private AlertDialog materialDialog;

    // This constructor means may copy from file, and may copy from zip
    public FileCopyDialog(Context context, String filePath,
                          String targetFolder, String apkPath, String decodeRootPath,
                          Map<String, String> entryMapping, int unused) {
        CopySource source = new CopySource();
        source.path = filePath;
        source.isInApk = false;
        source.isDir = new File(filePath).isDirectory();

        mCopySources = new ArrayList<>();
        mCopySources.add(source);
        mApkPath = apkPath;
        mDecodeRootPath = decodeRootPath;
        mEntryMapping = entryMapping;

        mTargetFolder = Objects.requireNonNullElseGet(targetFolder, () -> ScopedStorage.getApkEditorDir().getPath());

        handler = new MyHandler(context, this);

        init(context);
    }

    public FileCopyDialog(Context context, String apkPath,
                          String decodeRootPath, Map<String, String> fileEntry2ZipEntry,
                          List<CopySource> copySources, String targetFolder) {
        mApkPath = apkPath;
        mDecodeRootPath = decodeRootPath;
        mEntryMapping = fileEntry2ZipEntry;
        mCopySources = copySources;
        mTargetFolder = targetFolder;

        handler = new MyHandler(context, this);

        init(context);
    }

    @NonNull
    public static String getName(@NonNull String path) {
        int pos = path.lastIndexOf('/');
        return path.substring(pos + 1);
    }

    // Get the target saving file/dir which not exist, by adding (1), (2), etc
    @NonNull
    public static File getTargetNonExistFile(String path, boolean isDir) {
        int index = 1;
        String folder = null;
        String name = null;
        String fileType = null;
        if (!isDir) {
            int slashPos = path.lastIndexOf('/');
            folder = path.substring(0, slashPos + 1);
            String filename = path.substring(slashPos + 1);

            name = filename;
            fileType = "";
            int dotPos = filename.lastIndexOf('.');
            if (dotPos != -1) {
                name = filename.substring(0, dotPos);
                fileType = filename.substring(dotPos);
            }
        }

        while (true) {
            String testingPath = isDir ? path + "(" + index + ")" :
                    folder + name + "(" + index + ")" + fileType;
            File node = new File(testingPath);
            if (!node.exists()) {
                return node;
            }
            index += 1;
        }
    }

    private void init(Context context) {
        fileRenameOption = /*Integer.parseInt(LegacyPreferences.getFileRenameOption());*/ 2; //FIXME MOTHERFUCKER

        materialDialog = new MaterialAlertDialogBuilder(context).create();
        materialDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(android.R.string.ok), (dialog, which) -> {
            dialog.dismiss();
        });
    }

    // File copy succeed
    public void succeed(Context context) {
        // When just copy one file, show the full path
        if (mCopySources.size() == 1) {
            materialDialog.setMessage(String.format(context.getString(R.string.save_succeed_1), savedFilePath));
        } else {
            materialDialog.setMessage(String.format(context.getString(R.string.save_succeed_1), mTargetFolder));
        }
    }

    // File copy failed
    public void failed(@NonNull Context context, String msg) {
        materialDialog.setMessage(String.format(context.getString(R.string.failed_1), msg));
    }

    // Start file copy thread and show the dialog
    public void show() {
        new Thread(() -> {
            try {
                for (CopySource source : mCopySources) {
                    // The file/directory already decoded
                    if (!source.isInApk) {
                        copyFiles(source);
                    } else {
                        extractFiles(source);
                    }
                }
                handler.sendEmptyMessage(0);
                handler.post(() -> materialDialog.show());
            } catch (Exception e) {
                handler.setErrorMessage(e.getMessage());
                handler.sendEmptyMessage(1);
            }
        }).start();
    }

    // To extract file/directory from the apk/zip file
    protected void extractFiles(@NonNull CopySource source) throws Exception {
        String targetPath = mTargetFolder + "/" + getName(source.path);
        boolean bExist = new File(targetPath).exists();
        if (bExist && fileRenameOption == Constants.EXTRACT_AUTORENAME) {
            targetPath = getTargetNonExistPath(targetPath, source.isDir);
        }

        if (source.isDir) {
            ZipHelper.unzipDirectory(mApkPath, source.path, targetPath);
        } else {
            ZipHelper.unzipFileTo(mApkPath, source.path, targetPath);
        }

        savedFilePath = targetPath;
    }

    // Just copy the file/directory, as it is already decoded
    protected void copyFiles(@NonNull CopySource source) throws Exception {
        String targetPath = mTargetFolder + "/" + getName(source.path);
        File srcFile = new File(source.path);
        // Copy directory
        if (srcFile.isDirectory()) {
            File outputDirFile = new File(targetPath);
            if (outputDirFile.exists()) {
                if (fileRenameOption == Constants.EXTRACT_AUTORENAME) {
                    outputDirFile = createDirByAddSuffix(targetPath);
                }
            } else {
                outputDirFile.mkdirs();
            }
            copyDirectory(srcFile, outputDirFile);
            savedFilePath = outputDirFile.getPath();
        }
        // Copy file: just copy 1 file
        else {
            File dst = new File(targetPath);
            if (dst.exists()) {
                if (fileRenameOption == Constants.EXTRACT_AUTORENAME) {
                    dst = getTargetNonExistFile(targetPath, false);
                }
            }
            doFileCopy(new File(source.path), dst);
            savedFilePath = dst.getPath();
        }
    }

    // Get the target saving path which not exist, by adding (1), (2), etc
    @NonNull
    private String getTargetNonExistPath(String path, boolean isDir) {
        return getTargetNonExistFile(path, isDir).getPath();
    }

    // Create a new directory by adding (1) (2), ...
    @NonNull
    private File createDirByAddSuffix(String path) {
        File f = getTargetNonExistFile(path, true);
        f.mkdirs();
        return f;
    }

    // Copy all content in the directory
    // Assume the dstDir already exists
    private void copyDirectory(@NonNull File srcDir, File dstDir) throws Exception {
        File[] files = srcDir.listFiles();
        if (files != null)
            for (File f : files) {
                if (f.isFile()) {
                    File t = new File(dstDir, f.getName());
                    doFileCopy(f, t);
                } else {
                    File dir = new File(dstDir, f.getName());
                    dir.mkdir();
                    copyDirectory(f, dir);
                }
            }
    }

    // This is a customized copy
    // Note: if it is png/jpg file, will extract from zip
    private void doFileCopy(@NonNull File from, File to) throws Exception {
        String filename = from.getName();
        // It is the common image
        if (this.mApkPath != null
                && (filename.endsWith(".jpg") || (filename.endsWith(".png")
                && !filename.endsWith(".9.png")))) {
            String entryName = from.getPath().substring(
                    mDecodeRootPath.length() + 1);
            String realEntry = mEntryMapping.get(entryName);
            if (realEntry != null) {
                entryName = realEntry;
            }
            ZipHelper.unzipFileTo(this.mApkPath, entryName, to.getPath());
        } else {
            copyFile(from, to);
        }
    }

    public static class CopySource {
        public String path; // File path or entry path
        public boolean isDir;
        public boolean isInApk;
    }

    private static class MyHandler extends Handler {
        private final WeakReference<FileCopyDialog> dlgRef;
        private final Context mContext;
        private String errMsg;

        public MyHandler(Context context, FileCopyDialog dlg) {
            mContext = context;
            this.dlgRef = new WeakReference<>(dlg);
        }

        public void setErrorMessage(String msg) {
            this.errMsg = msg;
        }

        @Override
        public void handleMessage(Message msg) {
            FileCopyDialog dlg = dlgRef.get();
            if (dlg == null) {
                return;
            }
            switch (msg.what) {
                case 0:
                    dlg.succeed(mContext);
                    break;
                case 1:
                    dlg.failed(mContext, errMsg);
                    break;
            }
        }
    }
}