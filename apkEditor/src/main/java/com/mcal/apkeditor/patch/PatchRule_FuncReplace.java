package com.mcal.apkeditor.patch;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.ApkInfoActivity;
import com.mcal.apkeditor.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

class PatchRule_FuncReplace extends PatchRule {

    private static final String strEnd = "[/FUNCTION_REPLACE]";
    private static final String TARGET = "TARGET:";
    private static final String FUNCTION = "FUNCTION:";
    private static final String REPLACE = "REPLACE:";

    // Target file
    private String targetFile;
    // If the source is a zip, extract or not
    private String strFunction;
    private final List<String> replaceContents;
    private final List<String> keywords;

    PatchRule_FuncReplace() {
        replaceContents = new ArrayList<>();
        keywords = new ArrayList<>();
        keywords.add(strEnd);
        keywords.add(TARGET);
        keywords.add(FUNCTION);
        keywords.add(REPLACE);
    }

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
                this.targetFile = next.trim();
            } else if (FUNCTION.equals(line)) {
                String next = br.readLine();
                this.strFunction = next.trim();
            } else if (REPLACE.equals(line)) {
                line = readMultiLines(br, replaceContents, false, keywords);
                continue;
            } else {
                logger.error(R.string.patch_error_cannot_parse,
                        br.getCurrentLine(), line);
            }
            line = br.readLine();
        }
    }

    @Override
    public String executeRule(ApkInfoActivity activity, ZipFile patchZip,
                              @NonNull IPatchContext logger) {
        logger.error(R.string.general_error, "Not supported yet.");
        return null;
    }

    @Override
    public boolean isValid(IPatchContext logger) {
        return false;
    }

    @Override
    public boolean isSmaliNeeded() {
        return false;
    }

}
