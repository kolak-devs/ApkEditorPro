package com.mcal.apkeditor.patch.rules;

import static com.mcal.common.utils.FileHelperKt.copyFile;
import static com.mcal.common.utils.FileHelperKt.writeToFile;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.patch.LinedReader;
import com.mcal.apkeditor.patch.PatchRule;
import com.mcal.apkeditor.patch.interfaces.ApkInfoListener;
import com.mcal.apkeditor.patch.interfaces.IPatchContext;
import com.mcal.common.utils.HexHelper;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PatchRuleReviseSig extends PatchRule {

    private static final String strEnd = "[/SIGNATURE_REVISE]";
    private static final String TARGET = "TARGET:";

    private final List<String> targetList = new ArrayList<>();

    @Override
    public void parseFrom(@NonNull LinedReader br, IPatchContext logger) throws IOException {
        super.startLine = br.getCurrentLine();

        String line = br.readLine();
        while (line != null) {
            line = line.trim();
            if (strEnd.equals(line)) {
                break;
            }
            if (super.parseAsKeyword(line, br)) {
                line = br.readLine();
                continue;
            } else if (TARGET.equals(line)) {
                String next = br.readLine();
                String target = next.trim();
                targetList.add(target);
            } else {
                logger.error(R.string.patch_error_cannot_parse,
                        br.getCurrentLine(), line);
            }
            line = br.readLine();
        }
    }

    @Override
    public String executeRule(Activity activity, @NonNull ApkInfoListener listener, ZipFile patchZip, @NonNull IPatchContext logger) {
        String apkPath = listener.getApkPath();
        String hexRSA = getHexRSA(apkPath);
        String packageName = listener.getApkInfo().pkgName;
        String rootPath = logger.getDecodeRootPath();
        String targetFile = rootPath + "/" + targetList.get(0);

        try {
            String content = readFileContent(targetFile);
            content = content.replace("%PACKAGE_NAME%", packageName);
            content = content.replace("%RSA_DATA%", hexRSA);
            writeToFile(targetFile, content);
        } catch (Exception e) {
            logger.error(R.string.patch_error_write_to, targetFile);
        }

        //Log.d("DEBUG", "executeRule");
        return null;
    }

    // Get RSA data in hex from an apk file
    @Nullable
    private String getHexRSA(String apkPath) {
        ZipFile zfile = null;
        BufferedInputStream input = null;
        ByteArrayOutputStream output = null;

        try {
            zfile = new ZipFile(apkPath);
            Enumeration<?> zList = zfile.entries();
            ZipEntry ze;
            while (zList.hasMoreElements()) {
                ze = (ZipEntry) zList.nextElement();
                if (ze.isDirectory()) {
                    continue;
                }
                String entryName = ze.getName();
                if (!entryName.endsWith(".RSA") && !entryName.endsWith(".rsa") &&
                        !entryName.endsWith(".DSA") && !entryName.endsWith(".dsa")) {
                    continue;
                }

                // Found the RSA entry
                input = new BufferedInputStream(zfile.getInputStream(ze));
                output = new ByteArrayOutputStream();
                copyFile(input, output);
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(input);
            closeQuietly(output);
            closeQuietly(zfile);
        }

        if (output != null) {
            return HexHelper.bytesToHexString(output.toByteArray());
        } else {
            return null;
        }
    }

    @Override
    public boolean isValid(IPatchContext logger) {
        return !targetList.isEmpty();
    }

    // Always true as it patched smali files
    @Override
    public boolean isSmaliNeeded() {
        return true;
    }
}
