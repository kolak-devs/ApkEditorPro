package com.mcal.common.utilsOld;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.mcal.common.utilsOld.StorageUtils.StorageInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class SDCard {

    public static boolean exist() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    // Return like "/sdcard"
    @NonNull
    public static String getRootDirectory() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    public static void copyStream(@NonNull InputStream is, OutputStream os)
            throws IOException {
        byte[] buffer = new byte[4 * 1024];
        int count = 0;
        while ((count = is.read(buffer)) > 0) {
            os.write(buffer, 0, count);
        }
    }

    // The working directory is /sdcard/.ApkPermRemover/tmp/
    @NonNull
    public static String makeWorkingDir(Context ctx) throws Exception {
        return makeDir(ctx, "tmp");
    }

    @NonNull
    public static String makeBackupDir(Context ctx) throws Exception {
        return makeDir(ctx, "backup");
    }

    @NonNull
    public static String makeImageDir(Context ctx) throws Exception {
        return makeDir(ctx, "image");
    }

    @NonNull
    public static String makeDir(Context ctx, String dirName) throws Exception {
        if (!SDCard.exist()) {
            throw new Exception("Can not find sd card.");
        }

        String subDir = "";
        String packagePath = ctx.getPackageName();
        if (packagePath.startsWith("com.mcal.apkpermremover")) {
            subDir = "/.ApkPermRemover/" + dirName + "/";
        } else if (packagePath.startsWith("com.mcal.pmaster")) {
            subDir = "/PermMaster/" + dirName + "/";
        } else if (packagePath.equals("com.mcal.permissionmanager")) {
            subDir = "/PermMaster/" + dirName + "/";
        } else if (packagePath.startsWith("com.mcal.apkeditor")) {
            subDir = "/ApkEditor/" + dirName + "/";
        } else if (packagePath.startsWith("com.mcal.appdm")) {
            subDir = "/HackAppData/" + dirName + "/";
        }

        String rootDir = SDCard.getRootDirectory();
        String targetDir = rootDir + subDir;
        File f = new File(targetDir);
        if (!f.exists()) {
            f.mkdirs();
        }

        return targetDir;
    }

    @TargetApi(19)
    public static String getExternalStoragePath(Context ctx) {
        if (Build.VERSION.SDK_INT >= 19) {
            String internalPath = Environment.getExternalStorageDirectory().getPath();
            File[] files = ctx.getExternalFilesDirs(null);
            if (files != null) {
                // Find the pattern
                int appendedLen = 0;
                for (File f : files) {
                    String path = f.getPath();
                    if (path.startsWith(internalPath)) {
                        appendedLen = path.length() - internalPath.length();
                        break;
                    }
                }
                for (File f : files) {
                    String path = f.getPath();
                    if (!path.startsWith(internalPath)) {
                        return path.substring(0, path.length() - appendedLen);
                    }
                }
            }
        }

        String path = null;
        List<StorageInfo> storageList = StorageUtils.getStorageList();
        if (storageList != null) {
            for (StorageInfo si : storageList) {
                if (!si.internal && !si.readonly) {
                    path = si.path;
                    break;
                }
            }
        }
        return path;
    }

    private static String appDir = null;

    private static void initFolderName(Context ctx) {
        if (appDir != null) {
            return;
        }
        try {
            ApplicationInfo appInfo = ctx.getPackageManager()
                    .getApplicationInfo(ctx.getPackageName(),
                            PackageManager.GET_META_DATA);
            appDir = appInfo.metaData.getString("heagoo.sdcard_folder");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Backup directory is in SD card
    @NonNull
    public static String getBackupDir(Context ctx) {
        initFolderName(ctx);

        File f = Environment.getExternalStorageDirectory();
        String path = f.getPath() + "/" + appDir + "/backups";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }

    // Temp dir is in the internal storage
    @NonNull
    public static String getTempDir(Context ctx) {
        initFolderName(ctx);

        File f = Environment.getExternalStorageDirectory();
        String path = f.getPath() + "/" + appDir + "/temp";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return path;
    }

    @NonNull
    @SuppressLint("DefaultLocale")
    public static String getSizeDescription(long fileSize) {
        if (fileSize >= 1024 * 1024) {
            float mb = 1.0f * fileSize / 1024 / 1024;
            return String.format("%.2f M", mb);
        } else if (fileSize >= 1024) {
            float kb = 1.0f * fileSize / 1024;
            return String.format("%.2f K", kb);
        }

        return "" + fileSize + " B";
    }
}
