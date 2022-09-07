package com.mcal.apkeditor.patch;

import android.app.Activity;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.mcal.apkeditor.IGeneralCallback;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.patch.interfaces.ApkInfoListener;
import com.mcal.apkeditor.patch.interfaces.IPatchContext;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Keep
public class PatchExecutor implements IGeneralCallback {

    private final Activity activity;
    private final ApkInfoListener listener;
    private final String patchPath;
    private final IPatchContext patchContext;

    // patch and source zip file
    private Patch patch;
    private ZipFile sourceZip;

    public PatchExecutor(Activity activity, ApkInfoListener listener, String patchPath,
                         IPatchContext logger) {
        this.activity = activity;
        this.listener = listener;
        this.patchPath = patchPath;
        this.patchContext = logger;
    }

    public void applyPatch() {
        // Parse the patch
        try {
            this.sourceZip = new ZipFile(patchPath);
            ZipEntry entry = sourceZip.getEntry("patch.txt");
            if (entry == null) {
                sourceZip.close();
                sourceZip = null;
                patchContext.error(R.string.patch_error_no_entry, "patch.txt");
            }

            InputStream input = sourceZip.getInputStream(entry);
            this.patch = PatchParser.parse(input, patchContext);
            input.close();
        } catch (Exception e) {
            patchContext.error(R.string.general_error, e.getMessage());
            e.printStackTrace();
            return;
        }

        boolean needToDecode = false;

        // Check if need to decode DEX file
        if (!listener.isDexDecoded()) {
            for (PatchRule rule : patch.rules) {
                needToDecode = rule.isSmaliNeeded();
                if (needToDecode) {
                    break;
                }
            }

            if (needToDecode) {
                patchContext.info(R.string.decode_dex_file, true);
                listener.decodeDex(this);
            }
        }

        // When do not need to decode DEX, directly apply patch
        if (!needToDecode) {
            applyRules(patch.rules, sourceZip);
        }
    }

    private void applyRules(final List<PatchRule> rules, final ZipFile sourceZip) {
        new Thread() {
            @Override
            public void run() {
                // Apply all the rules
                int index = 0;
                while (index < rules.size()) {
                    PatchRule rule = rules.get(index);
                    patchContext.info(R.string.patch_start_apply, true, rule.startLine);

                    String nextRule = null;
                    if (rule.isValid(patchContext)) {
                        nextRule = rule.executeRule(activity, listener, sourceZip, patchContext);
                    }
                    // Goto the target rule
                    if (nextRule != null) {
                        index = findTargetRule(rules, nextRule);
                        continue;
                    }

                    index += 1;
                }
                patchContext.info(R.string.all_rules_applied, true);
                patchContext.patchFinished();
            }
        }.start();
    }

    // Get the index of the target rule
    private int findTargetRule(@NonNull List<PatchRule> rules, String name) {
        for (int i = 0; i < rules.size(); ++i) {
            if (name.equals(rules.get(i).getRuleName())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void callbackFunc() {
        if (patch != null && patch.rules != null && sourceZip != null) {
            applyRules(patch.rules, sourceZip);
        }
    }

    public List<String> getRuleNames() {
        List<String> names = new ArrayList<>();
        if (patch != null && patch.rules != null) {
            for (PatchRule rule : patch.rules) {
                names.add(rule.getRuleName());
            }
        }
        return names;
    }
}
