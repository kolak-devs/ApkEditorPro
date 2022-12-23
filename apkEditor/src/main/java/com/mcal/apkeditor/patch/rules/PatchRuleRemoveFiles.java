package com.mcal.apkeditor.patch.rules;

import static com.mcal.common.utils.FileHelperKt.deleteFile;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.patch.LinedReader;
import com.mcal.apkeditor.patch.PatchRule;
import com.mcal.apkeditor.patch.interfaces.ApkInfoListener;
import com.mcal.apkeditor.patch.interfaces.IPatchContext;
import com.mcal.common.utils.FileHelperKt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

public class PatchRuleRemoveFiles extends PatchRule {

    private static final String strEnd = "[/REMOVE_FILES]";
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
                String next;
                while ((next = br.readLine()) != null) {
                    next = next.trim();
                    if (next.startsWith("[")) {
                        break;
                    }
                    if (!"".equals(next)) {
                        this.targetList.add(next);
                    }
                }
                line = next;
                continue;
            } else {
                logger.error(R.string.patch_error_cannot_parse,
                        br.getCurrentLine(), line);
            }
            line = br.readLine();
        }
    }

    @Override
    public String executeRule(Activity activity, @NonNull ApkInfoListener listener, ZipFile patchZip,
                              IPatchContext logger) {
        String rootPath = listener.getDecodeRootPath();

        for (int i = 0; i < targetList.size(); ++i) {
            String targetPath = targetList.get(i);
            String filePath = rootPath + "/" + targetPath;
            int pos = filePath.lastIndexOf('/');
            String dirPath = filePath.substring(0, pos);
            String fileName = filePath.substring(pos + 1);

            try {
                File f = new File(filePath);
                File file1 = new File(dirPath, fileName);
                if (f.exists()) {
                    deleteFile(file1);
                } else {
                    FileHelperKt.deleteAll(file1);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public boolean isValid(IPatchContext logger) {
        if (targetList.isEmpty()) {
            logger.error(R.string.patch_error_no_target_file);
            return false;
        }
        return true;
    }

    @Override
    public boolean isSmaliNeeded() {
        for (String file : targetList) {
            if (super.isInSmaliFolder(file)) {
                return true;
            }
        }
        return false;
    }
}
