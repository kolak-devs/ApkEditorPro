package com.gmail.heagoo.apkeditor;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class NativeHelper {
    private static final String TAG = "NativeHelper";

    public static int isX86() {
        String[] abis = Build.SUPPORTED_ABIS;
        if (abis != null) {
            for (String abi : abis) {
                if (abi != null && abi.contains("x86")) {
                    return 1;
                }
            }
        }
        return 0;
    }

    public static void it(Context ctx, String pkgName, String dataDir, String apkPath) {
        // Integrity check - simplified for custom builds
        try {
            File apkFile = new File(apkPath);
            if (!apkFile.exists()) {
                Log.e(TAG, "APK file not found: " + apkPath);
                return;
            }
            long fileSize = apkFile.length();
            Log.d(TAG, "APK file size: " + fileSize);
            if (fileSize < 4000000) {
                Log.e(TAG, "APK file too small, possible tampering");
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "it() failed", e);
        }
    }

    public static int vc(Context ctx, int seed) {
        // Certificate verification - returns seed for custom builds
        return seed;
    }

    private static Map<String, String> parseMap(String str, int len) {
        Map<String, String> map = new LinkedHashMap<>();
        if (str == null || len <= 0) {
            return map;
        }
        String[] lines = str.split("\n", -1);
        for (int i = 0; i < lines.length - 1; i += 2) {
            String key = lines[i];
            String value = lines[i + 1];
            if (!key.isEmpty()) {
                map.put(key, value);
            }
        }
        return map;
    }

    private static List<String> parseList(String str, int len) {
        List<String> list = new ArrayList<>();
        if (str == null || len <= 0) {
            return list;
        }
        String[] lines = str.split("\n", -1);
        for (String line : lines) {
            if (!line.isEmpty()) {
                list.add(line);
            }
        }
        return list;
    }

    public static void mg(String tname, String sname,
                          String strReplace, int len2,
                          String strMapping, int len1) {
        Log.d(TAG, "mg() - merge: " + tname + " with " + sname);
        ZipOutputStream zos = null;
        ZipFile targetZip = null;
        ZipFile sourceZip = null;

        try {
            Map<String, String> mapping = parseMap(strMapping, len1);
            Map<String, String> replaces = parseMap(strReplace, len2);
            Log.d(TAG, "mapping size=" + mapping.size() + " replaces size=" + replaces.size());

            targetZip = new ZipFile(tname);
            sourceZip = new ZipFile(sname);

            File tempFile = new File(tname + ".tmp");
            zos = new ZipOutputStream(new FileOutputStream(tempFile));
            Set<String> processedEntries = new HashSet<>();

            // Process entries in target zip (resource APK)
            for (ZipEntry targetEntry : Collections.list(targetZip.entries())) {
                String entryName = targetEntry.getName();

                // Check if this entry should be replaced from file
                String replaceFile = replaces.get(entryName);
                if (replaceFile != null) {
                    Log.d(TAG, "Replace " + entryName + " with file " + replaceFile);
                    File replaceFileObj = new File(replaceFile);
                    if (replaceFileObj.exists()) {
                        ZipEntry newEntry = new ZipEntry(entryName);
                        newEntry.setMethod(ZipEntry.DEFLATED);
                        zos.putNextEntry(newEntry);
                        copyFile(new FileInputStream(replaceFileObj), zos);
                        zos.closeEntry();
                    }
                    processedEntries.add(entryName);
                    continue;
                }

                // Only replace common images (jpg/png)
                if (isCommonImage(entryName)) {
                    // Check if png is already replaced (not 68 bytes = not dummy)
                    if (isPngFile(entryName)) {
                        try {
                            ZipEntry srcEntry = sourceZip.getEntry(entryName);
                            if (srcEntry != null && srcEntry.getSize() != 68) {
                                continue; // Already has real content, skip
                            }
                        } catch (Exception e) {
                            // skip
                        }
                    }

                    // Replace from source using mapping
                    String sourceMappingName = mapping.get(entryName);
                    if (sourceMappingName == null) {
                        sourceMappingName = entryName;
                    }

                    ZipEntry sourceEntry = sourceZip.getEntry(sourceMappingName);
                    if (sourceEntry != null) {
                        ZipEntry newEntry = new ZipEntry(entryName);
                        newEntry.setMethod(sourceEntry.getMethod());
                        if (sourceEntry.getMethod() == ZipEntry.STORED) {
                            newEntry.setSize(sourceEntry.getSize());
                            newEntry.setCompressedSize(sourceEntry.getCompressedSize());
                            newEntry.setCrc(sourceEntry.getCrc());
                        }
                        zos.putNextEntry(newEntry);
                        InputStream is = sourceZip.getInputStream(sourceEntry);
                        copyFile(is, zos);
                        is.close();
                        zos.closeEntry();
                    }
                    processedEntries.add(entryName);
                }
                // Non-image entries are skipped (resources stay as-is from target)
            }

            // Copy non-resource files from source
            for (ZipEntry sourceEntry : Collections.list(sourceZip.entries())) {
                String entryName = sourceEntry.getName();
                if (processedEntries.contains(entryName)) {
                    continue;
                }
                if (isResourceFiles(entryName, mapping)) {
                    continue;
                }

                ZipEntry newEntry = new ZipEntry(entryName);
                newEntry.setMethod(sourceEntry.getMethod());
                if (sourceEntry.getMethod() == ZipEntry.STORED) {
                    newEntry.setSize(sourceEntry.getSize());
                    newEntry.setCompressedSize(sourceEntry.getCompressedSize());
                    newEntry.setCrc(sourceEntry.getCrc());
                }
                zos.putNextEntry(newEntry);
                InputStream is = sourceZip.getInputStream(sourceEntry);
                copyFile(is, zos);
                is.close();
                zos.closeEntry();
            }

            zos.close();
            zos = null;

            // Replace original with merged file
            targetZip.close();
            targetZip = null;
            sourceZip.close();
            sourceZip = null;

            File original = new File(tname);
            if (original.exists()) {
                original.delete();
            }
            tempFile.renameTo(original);
            Log.d(TAG, "mg() completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "mg() failed", e);
        } finally {
            try { if (zos != null) zos.close(); } catch (Exception e) {}
            try { if (targetZip != null) targetZip.close(); } catch (Exception e) {}
            try { if (sourceZip != null) sourceZip.close(); } catch (Exception e) {}
        }
    }

    public static void md(String tname, String sname,
                          String strAdded, int len1,
                          String strRemoved, int len2,
                          String strReplaced, int len3) {
        Log.d(TAG, "md() - modify zip: " + tname + " from " + sname);
        ZipOutputStream zos = null;
        ZipFile sourceZip = null;

        try {
            Map<String, String> added = parseMap(strAdded, len1);
            List<String> removed = parseList(strRemoved, len2);
            Map<String, String> replaced = parseMap(strReplaced, len3);
            Log.d(TAG, "add=" + added.size() + " remove=" + removed.size() + " replace=" + replaced.size());

            sourceZip = new ZipFile(sname);
            File tempFile = new File(tname + ".tmp");
            zos = new ZipOutputStream(new FileOutputStream(tempFile));

            Set<String> processedEntries = new HashSet<>();

            // Copy entries from source, applying remove and replace
            for (ZipEntry sourceEntry : Collections.list(sourceZip.entries())) {
                String entryName = sourceEntry.getName();

                // Skip removed entries
                if (removed.contains(entryName)) {
                    Log.d(TAG, "Removed: " + entryName);
                    continue;
                }

                // Check if this entry should be replaced from file
                String replaceFile = replaced.get(entryName);
                if (replaceFile != null) {
                    Log.d(TAG, "Replace " + entryName + " with " + replaceFile);
                    File replaceFileObj = new File(replaceFile);
                    if (replaceFileObj.exists()) {
                        ZipEntry newEntry = new ZipEntry(entryName);
                        newEntry.setMethod(ZipEntry.DEFLATED);
                        zos.putNextEntry(newEntry);
                        copyFile(new FileInputStream(replaceFileObj), zos);
                        zos.closeEntry();
                    }
                    processedEntries.add(entryName);
                    continue;
                }

                // Copy entry directly
                ZipEntry newEntry = new ZipEntry(entryName);
                newEntry.setMethod(sourceEntry.getMethod());
                if (sourceEntry.getMethod() == ZipEntry.STORED) {
                    newEntry.setSize(sourceEntry.getSize());
                    newEntry.setCompressedSize(sourceEntry.getCompressedSize());
                    newEntry.setCrc(sourceEntry.getCrc());
                }
                zos.putNextEntry(newEntry);
                InputStream is = sourceZip.getInputStream(sourceEntry);
                copyFile(is, zos);
                is.close();
                zos.closeEntry();
                processedEntries.add(entryName);
            }

            // Add new entries
            for (Map.Entry<String, String> entry : added.entrySet()) {
                String entryName = entry.getKey();
                String filePath = entry.getValue();
                Log.d(TAG, "Adding: " + entryName + " from " + filePath);
                File addFile = new File(filePath);
                if (addFile.exists()) {
                    ZipEntry newEntry = new ZipEntry(entryName);
                    newEntry.setMethod(ZipEntry.DEFLATED);
                    zos.putNextEntry(newEntry);
                    copyFile(new FileInputStream(addFile), zos);
                    zos.closeEntry();
                }
            }

            zos.close();
            zos = null;

            sourceZip.close();
            sourceZip = null;

            File original = new File(tname);
            if (original.exists()) {
                original.delete();
            }
            tempFile.renameTo(original);
            Log.d(TAG, "md() completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "md() failed", e);
        } finally {
            try { if (zos != null) zos.close(); } catch (Exception e) {}
            try { if (sourceZip != null) sourceZip.close(); } catch (Exception e) {}
        }
    }

    private static boolean isCommonImage(String fname) {
        if (fname == null || fname.length() <= 4) return false;
        String lower = fname.toLowerCase();
        if (lower.endsWith(".jpg")) return true;
        if (lower.endsWith(".png")) {
            if (fname.length() > 6 && fname.charAt(fname.length() - 5) == '9'
                    && fname.charAt(fname.length() - 6) == '.') {
                return false; // 9-patch
            }
            return true;
        }
        return false;
    }

    private static boolean isPngFile(String fname) {
        if (fname == null || fname.length() <= 4) return false;
        String lower = fname.toLowerCase();
        if (lower.endsWith(".png")) {
            if (fname.length() > 6 && fname.charAt(fname.length() - 5) == '9'
                    && fname.charAt(fname.length() - 6) == '.') {
                return false;
            }
            return true;
        }
        return false;
    }

    private static boolean isResourceFiles(String name, Map<String, String> mapping) {
        if (name == null) return false;
        if (name.startsWith("res/")) return true;
        if (name.startsWith("r/")) return true;
        if ("resources.arsc".equals(name)) return true;
        if ("AndroidManifest.xml".equals(name)) return true;
        if (mapping.containsValue(name)) return true;
        return false;
    }

    private static void copyFile(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[8192];
        int len;
        while ((len = is.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
    }

    // Helper to iterate ZipEntries (available from API level 1)
    private static class Collections {
        static <T> java.util.List<T> list(java.util.Enumeration<T> e) {
            java.util.ArrayList<T> list = new java.util.ArrayList<>();
            while (e.hasMoreElements()) {
                list.add(e.nextElement());
            }
            return list;
        }
    }
}
