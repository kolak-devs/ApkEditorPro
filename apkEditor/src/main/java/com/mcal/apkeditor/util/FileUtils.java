package com.mcal.apkeditor.util;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.mcal.common.utils.ScopedStorage;

import java.io.File;
import java.io.IOException;

public class FileUtils {
    @NonNull
    public static File createNewFile(File parent, String name) throws IOException {
        File createdFile = new File(parent, name);
        parent.mkdirs();
        createdFile.createNewFile();
        return createdFile;
    }

    @NonNull
    public static String makeBackupDir(Context ctx) throws Exception {
        return makeDir(ctx, "backup");
    }

    @NonNull
    public static String makeDir(Context ctx, String dirName) throws Exception {
        if (!exist()) {
            throw new Exception("Can not find sd card.");
        }

        String subDir = "";
        String packagePath = ctx.getPackageName();
        if (packagePath.startsWith("com.mcal.apkeditor.apkpermremover")) {
            subDir = "/.ApkPermRemover/" + dirName + "/";
        } else if (packagePath.startsWith("com.mcal.apkeditor.pmaster")) {
            subDir = "/PermMaster/" + dirName + "/";
        } else if (packagePath.equals("com.mcal.apkeditor.permissionmanager")) {
            subDir = "/PermMaster/" + dirName + "/";
        } else if (packagePath.startsWith("com.mcal.apkeditor")) {
            subDir = "/ApkEditor/" + dirName + "/";
        } else if (packagePath.startsWith("com.mcal.apkeditor.appdm")) {
            subDir = "/HackAppData/" + dirName + "/";
        } else if (packagePath.startsWith("com.mcal.apkeditor.pro")) {
            subDir = "/ApkEditor/" + dirName + "/";
        } else if (packagePath.startsWith("com.mcal.apkeditor.legacy")) {
            subDir = "/ApkEditor/" + dirName + "/";
        }

        String rootDir = ScopedStorage.getStorageDirectory().getPath();
        String targetDir = rootDir + subDir;
        File f = new File(targetDir);
        if (!f.exists()) {
            f.mkdirs();
        }

        return targetDir;
    }

    public static boolean exist() {
        if (android.os.Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }
}
