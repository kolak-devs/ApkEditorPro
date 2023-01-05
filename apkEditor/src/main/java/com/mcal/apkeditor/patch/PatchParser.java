package com.mcal.apkeditor.patch;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.patch.interfaces.IPatchContext;
import com.mcal.apkeditor.patch.rules.PatchRuleAddFiles;
import com.mcal.apkeditor.patch.rules.PatchRuleDummy;
import com.mcal.apkeditor.patch.rules.PatchRuleExecDex;
import com.mcal.apkeditor.patch.rules.PatchRuleFuncReplace;
import com.mcal.apkeditor.patch.rules.PatchRuleGoto;
import com.mcal.apkeditor.patch.rules.PatchRuleMatchAssign;
import com.mcal.apkeditor.patch.rules.PatchRuleMatchGoto;
import com.mcal.apkeditor.patch.rules.PatchRuleMatchReplace;
import com.mcal.apkeditor.patch.rules.PatchRuleMerge;
import com.mcal.apkeditor.patch.rules.PatchRuleRemoveFiles;
import com.mcal.apkeditor.patch.rules.PatchRuleReviseSig;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PatchParser {

    public static final String MIN_ENGINE_VER = "[MIN_ENGINE_VER]";
    public static final String AUTHOR = "[AUTHOR]";
    public static final String DESCRIPTION = "[DESCRIPTION]";
    public static final String PACKAGE = "[PACKAGE]";

    public static final String ADD_FILES = "[ADD_FILES]";
    public static final String REMOVE_FILES = "[REMOVE_FILES]";
    public static final String MERGE = "[MERGE]";
    public static final String MATCH_REPLACE = "[MATCH_REPLACE]";
    public static final String MATCH_GOTO = "[MATCH_GOTO]";
    public static final String MATCH_ASSIGN = "[MATCH_ASSIGN]";
    public static final String GOTO = "[GOTO]";
    public static final String DUMMY = "[DUMMY]";
    public static final String FUNCTION_REPLACE = "[FUNCTION_REPLACE]";
    public static final String SIGNATURE_REVISE = "[SIGNATURE_REVISE]";
    public static final String EXECUTE_DEX = "[EXECUTE_DEX]";

    @NonNull
    public static Patch parse(InputStream input, @NonNull IPatchContext logger)
            throws Exception {
        logger.info(R.string.patch_start_parse, true);
        Patch result = new Patch();

        LinedReader br = new LinedReader(new InputStreamReader(input));
        String line = br.readLine();
        while (line != null) {
            line = line.trim();

            // Start a tag
            if (line.startsWith("[")) {
                if (MIN_ENGINE_VER.equals(line)) {
                    String next = br.readLine();
                    result.requiredEngine = Integer.parseInt(next);
                } else if (AUTHOR.equals(line)) {
                    String next = br.readLine();
                    result.author = next;
                } else if (DESCRIPTION.equals(line)) {
                    String next = br.readLine();
                    result.description = next;
                } else if (PACKAGE.equals(line)) {
                    String next = br.readLine();
                    result.packagename = next;
                } else {
                    PatchRule rule = parseRule(br, line, logger);
                    if (rule != null) {
                        result.rules.add(rule);
                    }
                }
            } else if (line.startsWith("#") || "".equals(line)) {
                // comment or blank line
            } else {
                logger.error(R.string.patch_error_unknown_rule,
                        br.getCurrentLine(), line);
            }

            line = br.readLine();
        }

        return result;
    }

    // Запускаем парсинг правил внутри patch.txt
    private static PatchRule parseRule(LinedReader br, String startLine,
                                       IPatchContext logger) throws IOException {
        PatchRule rule = null;
        if (ADD_FILES.equals(startLine)) {
            rule = new PatchRuleAddFiles();
        } else if (REMOVE_FILES.equals(startLine)) {
            rule = new PatchRuleRemoveFiles();
        } else if (MERGE.equals(startLine)) {
            rule = new PatchRuleMerge();
        } else if (MATCH_REPLACE.equals(startLine)) {
            rule = new PatchRuleMatchReplace();
        } else if (MATCH_GOTO.equals(startLine)) {
            rule = new PatchRuleMatchGoto();
        } else if (MATCH_ASSIGN.equals(startLine)) {
            rule = new PatchRuleMatchAssign();
        } else if (FUNCTION_REPLACE.equals(startLine)) {
            rule = new PatchRuleFuncReplace();
        } else if (SIGNATURE_REVISE.equals(startLine)) {
            rule = new PatchRuleReviseSig();
        } else if (GOTO.equals(startLine)) {
            rule = new PatchRuleGoto();
        } else if (DUMMY.equals(startLine)) {
            rule = new PatchRuleDummy();
        } else if (EXECUTE_DEX.equals(startLine)) {
            rule = new PatchRuleExecDex();
        } else {
            logger.error(R.string.patch_error_unknown_rule,
                    br.getCurrentLine(), startLine);
        }
        if (rule != null) {
            rule.parseFrom(br, logger);
        }
        return rule;
    }
}
