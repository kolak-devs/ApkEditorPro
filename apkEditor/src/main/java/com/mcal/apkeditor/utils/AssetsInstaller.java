package com.mcal.apkeditor.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;

import androidx.annotation.NonNull;

import com.mcal.common.utils.IOUtils;
import com.mcal.common.data.Preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetsInstaller {
    private final Context context;

    public AssetsInstaller(@NonNull Context context) {
        this.context = context;
    }

    public void install() throws Exception {
        File path = new File(context.getFilesDir() + "/bin");
        if (!path.exists()) {
            path.mkdir();
        }
        AssetManager assets = context.getAssets();
        prepare(assets, path);
    }

    // This method will extract the necessary files
    public void prepare(AssetManager assets, File path) throws Exception {
        String curVersion = null;
        try {
            PackageInfo pInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            curVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // Prepare file
        boolean inited = Preferences.getInitialized();
        String lastVersion = Preferences.getVersionString();
        if (!inited || !lastVersion.equals(curVersion)) {
            copyKeyPk8(assets, path);
            copyKeyPem(assets, path);
            copyAapt(assets, path);
            copyAapt2(assets, path);
            copyAaptZ(assets, path);
            copyZipAlign(assets, path);
            copyAndroidJar(assets, path);
            copyMycp(assets);
            createWorkFiles();

            Preferences.setInitialized(true);
            Preferences.setVersionString(curVersion);
        }
    }

    private void createWorkFiles() {
        // If need to limit the new version, does not need to create such files
        File f = new File(context.getFilesDir(), "work.xml");
        if (!f.exists()) {
            try {
                f.createNewFile();
                f.setWritable(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        f = new File(context.getFilesDir(), "work.db");
        if (!f.exists()) {
            try {
                f.createNewFile();
                f.setWritable(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void copyMycp(@NonNull AssetManager assets) throws IOException {
        // Copy mycp
        try {
            File bin = new File(context.getFilesDir(), "mycp");
            if (!bin.exists()) {
                InputStream input = assets.open(Build.CPU_ABI + "/mycp");
                FileOutputStream output = new FileOutputStream(bin);
                IOUtils.copy(input, output);
                input.close();
                output.close();
                bin.setExecutable(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void copyKeyPk8(@NonNull AssetManager assets, File outDir) throws IOException {
        File pk8 = new File(outDir, "testkey.pk8");
        InputStream is = assets.open("key/testkey.pk8");
        OutputStream os = new FileOutputStream(pk8);
        IOUtils.copy(is, os);
        is.close();
        os.close();
    }

    private void copyKeyPem(@NonNull AssetManager assets, File outDir) throws IOException {
        File pk8 = new File(outDir, "testkey.x509.pem");
        InputStream is = assets.open("key/testkey.x509.pem");
        OutputStream os = new FileOutputStream(pk8);
        IOUtils.copy(is, os);
        is.close();
        os.close();
    }

    private void copyAaptZ(@NonNull AssetManager assets, File outDir) throws IOException {
        File aapt2 = new File(outDir, "aaptz");
        InputStream aaptz_in = assets.open("aaptz");
        OutputStream aaptz_out = new FileOutputStream(aapt2);
        IOUtils.copy(aaptz_in, aaptz_out);
        aaptz_in.close();
        aaptz_out.close();
        aapt2.setExecutable(true);
    }

    private void copyAapt(@NonNull AssetManager assets, File outDir) throws IOException {
        File aapt = new File(outDir, "aapt");
        InputStream aapt_in = assets.open(Build.CPU_ABI + "/aapt");
        OutputStream aapt_out = new FileOutputStream(aapt);
        IOUtils.copy(aapt_in, aapt_out);
        aapt_in.close();
        aapt_out.close();
        aapt.setExecutable(true);
    }

    private void copyAapt2(@NonNull AssetManager assets, File outDir) throws IOException {
        File aapt2 = new File(outDir, "aapt2");
        InputStream aapt2_in = assets.open(Build.CPU_ABI + "/aapt2");
        OutputStream aapt2_out = new FileOutputStream(aapt2);
        IOUtils.copy(aapt2_in, aapt2_out);
        aapt2_in.close();
        aapt2_out.close();
        aapt2.setExecutable(true);
    }

    private void copyZipAlign(@NonNull AssetManager assets, File outDir) throws IOException {
        File aapt2 = new File(outDir, "zipalign");
        InputStream aapt2_in = assets.open(Build.CPU_ABI + "/zipalign");
        OutputStream aapt2_out = new FileOutputStream(aapt2);
        IOUtils.copy(aapt2_in, aapt2_out);
        aapt2_in.close();
        aapt2_out.close();
        aapt2.setExecutable(true);
    }

    // Copy android-framework.jar
    private void copyAndroidJar(@NonNull AssetManager assets, File outDir) throws IOException {
        File aapt2 = new File(outDir, "android-framework.jar");
        InputStream aapt2_in = assets.open("android-framework.jar");
        OutputStream aapt2_out = new FileOutputStream(aapt2);
        IOUtils.copy(aapt2_in, aapt2_out);
        aapt2_in.close();
        aapt2_out.close();
        aapt2.setExecutable(true);
    }
}