package com.mcal.apkeditor.patch.rules;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.patch.LinedReader;
import com.mcal.apkeditor.patch.PatchRule;
import com.mcal.apkeditor.patch.interfaces.ApkInfoListener;
import com.mcal.apkeditor.patch.interfaces.IPatchContext;

import java.io.IOException;
import java.util.zip.ZipFile;


public class PatchRuleDummy extends PatchRule {

    private static final String strEnd = "[/DUMMY]";

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
            } else {
                logger.error(R.string.patch_error_cannot_parse,
                        br.getCurrentLine(), line);
            }
            line = br.readLine();
        }
    }

    @Override
    public String executeRule(Activity activity, ApkInfoListener listener, ZipFile patchZip, IPatchContext logger) {
        return null;
    }

    @Override
    public boolean isValid(IPatchContext logger) {
        return true;
    }

    @Override
    public boolean isSmaliNeeded() {
        return false;
    }
}
