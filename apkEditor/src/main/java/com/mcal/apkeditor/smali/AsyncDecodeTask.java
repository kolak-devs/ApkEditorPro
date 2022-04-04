package com.mcal.apkeditor.smali;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.pro.DexDecoder;
import com.mcal.common.utils.IOUtils;
import com.mcal.common.utils.SDCard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AsyncDecodeTask extends AsyncTask<Void, Integer, Boolean> {

    @SuppressLint("StaticFieldLeak")
    private final Context context;
    private final IDecodeTaskCallback callback;
    private final String decodeRootPath;
    private final String apkPath;
    // Record all dex file paths which are extracted from apk file
    private final List<String> dexFileList = new ArrayList<>();
    private String strError;
    private String strWarning;

    public AsyncDecodeTask(Context context, String apkPath,
                           String decodeRootPath, IDecodeTaskCallback callback) {
        this.context = context;
        this.callback = callback;
        this.apkPath = apkPath;
        this.decodeRootPath = decodeRootPath;
    }

    @Override
    protected void onPreExecute() {
        if (callback != null) {
            callback.dexDecodingStarted();
        }
    }

    private void prepareDexFiles() throws Exception {
        String tmpDirectory = SDCard.makeDir(context, "tmp");

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(apkPath);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".dex") && !name.contains("/")) {
                    unzipDex2File(zipFile, entry, tmpDirectory + name);
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void prepareMainDex() throws Exception {
        String tmpDirectory = SDCard.makeDir(context, "tmp");

        ZipFile zipFile = null;
        try {
            String name = "classes.dex";
            zipFile = new ZipFile(apkPath);
            ZipEntry entry = zipFile.getEntry(name);
            unzipDex2File(zipFile, entry, tmpDirectory + name);
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void unzipDex2File(@NonNull ZipFile zipFile, ZipEntry entry, String filePath)
            throws IOException {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = zipFile.getInputStream(entry);
            out = new FileOutputStream(filePath);
            IOUtils.copy(in, out);
            this.dexFileList.add(filePath);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            doAllJobs();
            return true;
        } catch (Exception e) {
            this.strError = e.getMessage();
            return false;
        }
    }

    public void doAllJobs() throws Exception {
        prepareDexFiles();
        decodeDexFiles();
        removeDexFiles();
    }

    // Only decode the classes.dex
    public void decodeMainDex() throws Exception {
        prepareMainDex();
        decodeDexFiles();
        removeDexFiles();
    }

    private void removeDexFiles() {
        for (String filePath : this.dexFileList) {
            File f = new File(filePath);
            f.delete();
        }
    }

    private void decodeDexFiles() {
        for (String dexFilePath : this.dexFileList) {
            DexDecoder decoder = new DexDecoder(dexFilePath);

            String directory = this.decodeRootPath + "/smali";
            // Not the default dex file
            if (!dexFilePath.endsWith("/classes.dex")) {
                int position = dexFilePath.lastIndexOf("/");
                String dexName = dexFilePath.substring(position + 1,
                        dexFilePath.length() - 4);
                directory = this.decodeRootPath + "/smali_" + dexName;
            }
            createDirectoryIfNotExist(directory);

            try {
                decoder.dex2smali(directory);
            } catch (Exception e) {
                e.printStackTrace();
            }


            if (this.strWarning == null) {
                this.strWarning = decoder.getWarning();
            }
        }
    }

    private void createDirectoryIfNotExist(String directory) {
        File dir = new File(directory);
        if (dir.exists()) {
            dir.mkdir();
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (callback != null) {
            if (result) {
                for (File file : new File(decodeRootPath).listFiles()) {
                    if (file.getName().endsWith(".dex")) {
                       if(file.delete()) {
                           Log.e(getClass().getName(), file + " deleted");
                       }
                    }
                }
                callback.dexDecodingFinished(true, null, strWarning);
            } else {
                callback.dexDecodingFinished(false, strError, null);
            }
        }
    }

    public interface IDecodeTaskCallback {
        void dexDecodingStarted();

        void dexDecodingFinished(boolean result, String strError,
                                        String strWarning);
    }
}
