package com.mcal.common.utilsOld;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public class ApkInfoParser {

    public ApkInfoParser() {
    }

    public AppInfo parse(@NonNull Context ctx, String apkPath) throws Exception {
        AppInfo apkInfo = null;

        PackageManager packageManager = ctx.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(apkPath, 0);
        if (packageInfo != null) {
            apkInfo = new AppInfo();
            packageInfo.applicationInfo.sourceDir = apkPath;
            packageInfo.applicationInfo.publicSourceDir = apkPath;
            apkInfo.label = packageInfo.applicationInfo.loadLabel(packageManager).toString();
            apkInfo.packageName = packageInfo.packageName;
            apkInfo.icon = packageInfo.applicationInfo.loadIcon(packageManager);
        }

        return apkInfo;
    }

    public static class AppInfo {
        public String label;
        public String packageName;
        public Drawable icon;
    }
}
