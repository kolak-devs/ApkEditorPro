package com.mcal.apkeditor.patch.rules;

import static com.mcal.common.utils.FileHelperKt.copyFile;
import static com.mcal.common.utils.StringHelperKt.getRandomString;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.patch.LinedReader;
import com.mcal.apkeditor.patch.PatchRule;
import com.mcal.apkeditor.patch.interfaces.ApkInfoListener;
import com.mcal.apkeditor.patch.interfaces.IPatchContext;
import com.mcal.common.utils.ScopedStorage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PatchRuleAddFiles extends PatchRule {

    private static final String strEnd = "[/ADD_FILES]";
    private static final String SOURCE = "SOURCE:";
    private static final String TARGET = "TARGET:";
    private static final String EXTRACT = "EXTRACT:";

    // Source file in the patched zip
    private String sourceFile;
    // Target place
    private String targetFile;
    // If the source is a zip, extract or not
    private boolean bExtract;

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
            } else if (SOURCE.equals(line)) {
                String next = br.readLine();
                this.sourceFile = next.trim();
            } else if (TARGET.equals(line)) {
                String next = br.readLine();
                this.targetFile = next.trim();
            } else if (EXTRACT.equals(line)) {
                String next = br.readLine();
                this.bExtract = Boolean.parseBoolean(next.trim());
            } else {
                logger.error(R.string.patch_error_cannot_parse, br.getCurrentLine(), line);
            }
            line = br.readLine();
        }

        if (targetFile != null && targetFile.endsWith("/")) {
            targetFile = targetFile.substring(0, targetFile.length() - 1);
        }
    }

    @Override
    public String executeRule(Activity activity, ApkInfoListener listener, @NonNull ZipFile patchZip,
                              IPatchContext logger) {

        ZipEntry entry = patchZip.getEntry(sourceFile);
        if (entry == null) {
            logger.error(R.string.patch_error_no_entry, sourceFile);
            return null;
        }

        InputStream input = null;
        FileOutputStream fos = null;
        try {
            input = patchZip.getInputStream(entry);

            // Directly copy the content
            if (!this.bExtract) {
                String targetPath = listener.getDecodeRootPath() + "/"
                        + targetFile;
                listener.getResListAdapter().addFile(targetPath, input);
            }
            // Source is a zip file
            else {
                String tmpDir = ScopedStorage.getTmpDir().getPath();
                String path = tmpDir + getRandomString(6);
                fos = new FileOutputStream(path);
                copyFile(input, fos);
                fos.close();
                fos = null;

                // Extract files in zip to target folder
                addFilesInZip(activity, listener, path, null, logger);
            }
        } catch (Exception e) {
            logger.error(R.string.general_error, e.getMessage());
        } finally {
            closeQuietly(input);
            closeQuietly(fos);
        }

        return null;
    }

    @Override
    public boolean isSmaliNeeded() {
        return super.isInSmaliFolder(this.targetFile);
    }

    @Override
    public boolean isValid(IPatchContext logger) {
        if (sourceFile == null) {
            logger.error(R.string.patch_error_no_source_file);
            return false;
        }
        if (targetFile == null) {
            logger.error(R.string.patch_error_no_target_file);
            return false;
        }
        return true;
    }
}
