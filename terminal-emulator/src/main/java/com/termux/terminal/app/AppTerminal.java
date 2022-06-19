package com.termux.terminal.app;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.blankj.utilcode.util.FileUtils;
import com.mcal.common.AppCommon;
import com.termux.terminal.app.manager.ToolsManager;
import com.termux.terminal.app.utils.Environment;

import java.io.File;
import java.util.Arrays;

public class AppTerminal extends AppCommon {
    @SuppressLint("StaticFieldLeak")
    private static AppTerminal instance;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Environment.init();
        ToolsManager.init(null);
    }

    public static AppTerminal getBaseInstance() {
        return instance;
    }

    public static boolean isAarch64() {
        return Arrays.asList(Build.SUPPORTED_ABIS).contains("arm64-v8a");
    }

    public static boolean isArmv7a() {
        return Arrays.asList(Build.SUPPORTED_ABIS).contains("armeabi-v7a");
    }

    @Nullable
    public static String getArch() {
        if (isAarch64()) {
            return "arm64-v8a";
        } else if (isArmv7a()) {
            return "armeabi-v7a";
        }
        return null;
    }

    public File getRootDir() {
        return new File(getRootFilesDir(), "home");
    }

    @SuppressLint("SdCardPath")
    public File getRootFilesDir() {
        return mkdirIfNotExits(getFilesDir());
    }

    public static File mkdirIfNotExits(File in) {
        if (in != null && !in.exists()) {
            FileUtils.createOrExistsDir(in);
        }
        return in;
    }
}
