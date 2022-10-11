package com.mcal.common.fastzip;

import androidx.annotation.NonNull;

import com.mcal.common.fastzip.utils.CRC32Utils;
import com.mcal.common.fastzip.utils.MyBase64;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FastZip {
    /*public static void extract(File zip, @NonNull File extractDir) throws IOException {
        extractDir.mkdirs();
        ZipFile apk = new ZipFile(zip);
        Enumeration<? extends ZipEntry> entries = apk.entries();
        Log.e("FastZip", "Extracting");
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            Log.e("FastZip", "Entry: " + entry.getName());
            String name = entry.getName();
            //extract only manifest and dex files
            if (name.equals("resources.arsc") && !entry.isDirectory()) {
                BufferedInputStream bis = new BufferedInputStream(apk.getInputStream(entry));
                File f = new File(extractDir, entry.getName());
                if (!f.exists()) f.createNewFile();
                FileOutputStream fos = new FileOutputStream(f);
                byte[] buffer = new byte[2048];
                int len = 0;
                while ((len = bis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                Log.e("FastZip", "Success extract: " + extractDir.getAbsolutePath() + File.separator + entry.getName());
            }
        }
    }*/

    public static void repack(File inZip, File outZip, Map<String, String> replacedFiles,
                              Map<String, String> addedFiles, Set<String> deletedFiles) throws Exception {
        ZipFile zipFile = new ZipFile(inZip);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        FastZipOutputStream fzos = new FastZipOutputStream(new BufferedOutputStream(new FileOutputStream(outZip)));

        Manifest manifest = addDigestsToManifest(new JarFile(new File(inZip.getAbsolutePath()), false),
                replacedFiles, addedFiles, deletedFiles);
        Map<String, Attributes> entries2 = manifest.getEntries();
        List<String> names = new ArrayList<>(entries2.keySet());
        Collections.sort(names);

        // Replaced, Added, Deleted files
        for (String name : names) {
            if (replacedFiles.containsKey(name)) {
                String filePath = replacedFiles.get(name);
                JarEntry outEntry = new JarEntry(name);
                byte[] buffer = new byte[2048];
                int lenReplacedFiles = 0;

                if (CRC32Utils.isNoCompressFileType(name)) {
                    outEntry.setMethod(ZipEntry.STORED);
                    outEntry.setSize(new FileInputStream(filePath).available());
                    outEntry.setCrc(CRC32Utils.calculateCrc(new FileInputStream(filePath)).getValue());
                    outEntry.setCompressedSize(-1);

                    lenReplacedFiles += 30 + name.length();
                    int needed = (4 - (lenReplacedFiles % 4)) % 4;
                    if (needed != 0) {
                        outEntry.setExtra(new byte[needed]);
                    }
                } else {
                    outEntry.setMethod(ZipEntry.DEFLATED);
                }

                BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(filePath));
                fzos.putNextEntry(outEntry);
                while ((lenReplacedFiles = fileInput.read(buffer)) > 0) {
                    fzos.write(buffer, 0, lenReplacedFiles);
                }
                fzos.closeEntry();
            }
        }

        //repack files from original apk
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();

            if (name.startsWith("META-INF/")) continue;
            fzos.copyZipEntry(entry, zipFile);
        }
        fzos.closeEntry();
        fzos.close();
    }

    @NonNull
    private static Manifest addDigestsToManifest(@NonNull JarFile jar,
                                                 Map<String, String> replaces, Map<String, String> addedAssetFiles,
                                                 Set<String> deletedFiles) throws IOException,
            GeneralSecurityException {
        Manifest input = jar.getManifest();
        Manifest output = new Manifest();
        Attributes main = output.getMainAttributes();
        if (input != null) {
            main.putAll(input.getMainAttributes());
        } else {
            main.putValue("Manifest-Version", "1.0");
            main.putValue("Created-By", "1.0 (Android SignApk)");
        }

        MessageDigest md = MessageDigest.getInstance("SHA1");
        byte[] buffer = new byte[4096];
        int num;

        // We sort the input entries by name, and add them to the
        // output manifest in sorted order. We expect that the output
        // map will be deterministic.

        TreeMap<String, JarEntry> byName = new TreeMap<>();

        for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
            JarEntry entry = e.nextElement();
            byName.put(entry.getName(), entry);
        }

        for (JarEntry entry : byName.values()) {
            String name = entry.getName();
            // Added 20160410: skip the removed entry
            if (deletedFiles != null && deletedFiles.contains(name)) {
                continue;
            }

            if (!entry.isDirectory()) {
                // Modified by Pujiang
                if (replaces.containsKey(name)) {
                    FileInputStream fis = new FileInputStream(
                            replaces.get(name));
                    while ((num = fis.read(buffer)) > 0) {
                        md.update(buffer, 0, num);
                    }
                    fis.close();
                } else {
                    InputStream data = jar.getInputStream(entry);
                    while ((num = data.read(buffer)) > 0) {
                        md.update(buffer, 0, num);
                    }
                }

                Attributes attr = new Attributes();
                String digest = MyBase64.encode(md.digest());
                attr.putValue("SHA1-Digest", digest);
                output.getEntries().put(name, attr);
            }
        }

        // For new added files
        if (addedAssetFiles != null) {
            for (String name : addedAssetFiles.keySet()) {
                String filePath = addedAssetFiles.get(name);
                FileInputStream fis = new FileInputStream(filePath);
                while ((num = fis.read(buffer)) > 0) {
                    md.update(buffer, 0, num);
                }
                fis.close();

                Attributes attr = new Attributes();
                attr.putValue("SHA1-Digest", MyBase64.encode(md.digest()));
                output.getEntries().put(name, attr);
            }
        }
        return output;
    }

    public static void repack(@NotNull String inZip, @NotNull String outZip, @Nullable Map<String, String> replacedFiles,
                              @Nullable Map<String, String> addedFiles, @Nullable Set<String> deletedFiles) throws Exception {
        repack(new File(inZip), new File(outZip), replacedFiles, addedFiles, deletedFiles);
    }
}