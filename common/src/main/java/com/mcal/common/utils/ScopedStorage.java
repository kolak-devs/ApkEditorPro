package com.mcal.common.utils;

import android.os.Environment;

import androidx.annotation.NonNull;

import com.mcal.common.App;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.util.List;

public class ScopedStorage {
    @NonNull
    @Contract(" -> new")
    public static File getApkEditorDirectory() {
        return new File(Environment.getExternalStorageDirectory() + "/ApkEditor");
    }

    public static File getFilesDir() {
        return App.getContext().getFilesDir();
    }

    public static String getExternalStoragePath() {
        String internalPath = ScopedStorage.getStorageDirectory().getPath();
        File[] files = App.getContext().getExternalFilesDirs(null);
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

        String path = null;
        List<StorageUtils.StorageInfo> storageList = StorageUtils.getStorageList();
        if (storageList != null) {
            for (StorageUtils.StorageInfo si : storageList) {
                if (!si.internal && !si.readonly) {
                    path = si.path;
                    break;
                }
            }
        }
        return path;
    }

    public static File getStorageDirectory() {
        return Environment.getExternalStorageDirectory();
    }
}
