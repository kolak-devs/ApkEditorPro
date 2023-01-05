package com.mcal.apkeditor.utils;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    private static void addFolderToZip(String path, String srcFolder,
                                       ZipOutputStream zip) throws IOException {
        File folder = new File(srcFolder);
        String[] subFiles = folder.list();
        if (subFiles == null || subFiles.length == 0) {
            addFileToZip(path, srcFolder, zip, true);
        } else {
            for (String fileName : subFiles) {
                if (path.equals("")) {
                    addFileToZip(folder.getName(), srcFolder + "/" + fileName,
                            zip, false);
                } else {
                    addFileToZip(path + "/" + folder.getName(), srcFolder + "/"
                            + fileName, zip, false);
                }
            }
        }
    }

    private static void addFileToZip(String path, String srcFile,
                                     ZipOutputStream zip, boolean flag) throws IOException {
        File folder = new File(srcFile);
        if (flag) {
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName() + "/"));
        } else {
            if (folder.isDirectory()) {
                addFolderToZip(path, srcFile, zip);
            } else {
                byte[] buf = new byte[4096];
                int len;
                FileInputStream in = new FileInputStream(srcFile);
                zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
                in.close();
            }
        }
    }

    private static int getSlashNum(@NonNull String str) {
        int slashNum = 0;
        int startOff = 0;
        while ((startOff = str.indexOf('/', startOff)) != -1) {
            slashNum += 1;
            startOff += 1;
        }
        return slashNum;
    }

    // prefix like "res/", means to list all the entries under res directory
    // prefix like "res/" will return "res/a.png", but will not return "res/raw/a.png"
    @NonNull
    public static List<String> listFiles(String zipPath, String prefix) {
        List<String> result = new ArrayList<>();
        int slashNum = getSlashNum(prefix);

        ZipFile zfile = null;
        try {
            zfile = new ZipFile(zipPath);
            Enumeration<?> zList = zfile.entries();
            ZipEntry ze;
            while (zList.hasMoreElements()) {
                ze = (ZipEntry) zList.nextElement();
                if (!ze.isDirectory()) {
                    String name = ze.getName();
                    if (name.startsWith(prefix) && getSlashNum(name) == slashNum) {
                        result.add(name);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zfile != null) {
                try {
                    zfile.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }
}