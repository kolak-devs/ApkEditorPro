package com.mcal.apkeditor.se;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.mcal.apkeditor.BuildConfig;
import com.mcal.common.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ApkInstaller {
    public static void install(@NonNull Context ctx, String targetApkPath) {
        Uri fileUri = null;
        Intent intent;
        intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        File dir = ctx.getExternalFilesDir("apk");
        if (!dir.exists()) {
            dir.mkdir();
        }
        File apk = new File(dir, "gen.apk");
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(targetApkPath);
            outputStream = new FileOutputStream(apk);
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            Toast.makeText(ctx, "Internal error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
        try {
            fileUri = FileProvider.getUriForFile(
                    ctx, BuildConfig.APPLICATION_ID, apk);
        } catch (Throwable ignored) {
        }
        if (fileUri != null) {
            try {
                intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ctx.startActivity(intent);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
