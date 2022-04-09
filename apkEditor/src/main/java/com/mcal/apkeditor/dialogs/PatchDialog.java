package com.mcal.apkeditor.dialogs;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mcal.apkeditor.activities.ApkInfoActivity;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.ce.ManifestInfo;
import com.mcal.apkeditor.ce.ManifestParser;
import com.mcal.apkeditor.dialogs.FileSelectDialog.IFileSelection;
import com.mcal.apkeditor.patch.IPatchContext;
import com.mcal.apkeditor.patch.PatchExecutor;
import com.mcal.apkeditor.view.ViewDialog;
import com.mcal.common.utils.IOUtils;
import com.mcal.common.utils.SDCard;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// Dialog used for patch applying
public class PatchDialog
        implements android.view.View.OnClickListener, IPatchContext {

    private final WeakReference<ApkInfoActivity> activityRef;
    // Record all the global parameter values
    private final Map<String, String> globalVariableValues = new HashMap<String, String>();
    private String exampleDir = null;
    private String patchPath = null;
    private TextView curPatchTv;
    private TextView patchPathTv;
    private TextView saveExamplesTv;
    private Button selectApplyBtn;
    private WebView webView;
    private View logLayout;
    private TextView logTv;
    // Manifest info parsed from original apk
    private ManifestInfo manifestInfo;
    // Record executor as the parse is done there
    private PatchExecutor patchExecutor;
    private ViewDialog dialog;

    public PatchDialog(ApkInfoActivity activity) {

        this.activityRef = new WeakReference<>(activity);

        init(activity);
    }

    private void init(ApkInfoActivity activity) {

        View view = LayoutInflater.from(activity).inflate(R.layout.dlg_patch, null);

        this.curPatchTv = view.findViewById(R.id.tv_curpatch);
        curPatchTv.setOnClickListener(this);
        this.patchPathTv = view.findViewById(R.id.tv_patch_path);
        Button closeBtn = view.findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);
        this.selectApplyBtn = view.findViewById(R.id.btn_select_apply);
        selectApplyBtn.setOnClickListener(this);
        this.saveExamplesTv = view
                .findViewById(R.id.tv_save_patches);
        saveExamplesTv.setOnClickListener(this);

        this.webView = view.findViewById(R.id.web_instructions);
        webView.loadUrl("file:///android_res/raw/about_patch.html");
        this.logLayout = view.findViewById(R.id.log_layout);
        this.logTv = view.findViewById(R.id.tv_patchlog);

        dialog = new ViewDialog(activity);
        dialog.setTitle(R.string.patch_capital);
        dialog.setView(view);
    }

    public void show() {
        dialog.show();
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            dialog.dismiss();
        }
        // Click on current patch
        else if (id == R.id.tv_curpatch) {
            selectPatch();
        }
        // Select a patch or apply it
        else if (id == R.id.btn_select_apply) {
            if (patchPath == null) {
                selectPatch();
            } else {
                applyPatch();
            }
        }
        // Extract and save the examples
        else if (id == R.id.tv_save_patches) {
            boolean ret = extractExamples("patch_app_rename.zip");
            ret |= extractExamples("patch_data_editor.zip");
            ret |= extractExamples("patch_new_entrance.zip");
            ret |= extractExamples("patch_my_font.zip");
            ret |= extractExamples("patch_mem_editor.zip");
            ret |= extractExamples("patch_bypass_sigcheck.zip");
            ret |= extractExamples("patch_launcher_toast.zip");
            ret |= extractExamples("patch_script_example.zip");
            if (ret) {
                String message = String.format(
                        activityRef.get()
                                .getString(R.string.patch_examples_copied),
                        this.exampleDir);
                Toast.makeText(activityRef.get(), message, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void initExampleDir() {
        // Check the directory exist or not
        if (this.exampleDir == null) {
            try {
                exampleDir = SDCard.makeDir(activityRef.get(), "examples");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    private boolean extractExamples(String filename) {
        initExampleDir();

        String path = exampleDir + filename;

        AssetManager am = activityRef.get().getAssets();
        InputStream input = null;
        FileOutputStream output = null;
        try {
            input = am.open(filename);
            output = new FileOutputStream(path);
            IOUtils.copy(input, output);

            return true;
        } catch (IOException e) {
            Toast.makeText(activityRef.get(), e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        } finally {
            closeQuietly(input);
            closeQuietly(output);
        }

        return false;
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void closeQuietly(ZipFile zfile) {
        if (zfile != null) {
            try {
                zfile.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void applyPatch() {
        // Switch the view
        logTv.setText("");
        selectApplyBtn.setEnabled(false);

        // Patch it
        patchExecutor = new PatchExecutor(activityRef.get(), patchPath, this);
        patchExecutor.applyPatch();
    }

    private void selectPatch() {
        String defaultDir = null;
        initExampleDir();
        if (new File(exampleDir).exists()) {
            defaultDir = exampleDir;
        }
        new FileSelectDialog(activityRef.get(),
                new IFileSelection() {
                    @Override
                    public void fileSelectedInDialog(
                            String filePath, String extraStr, boolean openFile) {
                        patchSelected(filePath);
                    }

                    @Override
                    public boolean isInterestedFile(String filename, String extraStr) {
                        return filename.endsWith(".zip");
                    }

                    @Override
                    public String getConfirmMessage(String filePath,
                                                    String extraStr) {
                        return null;
                    }
                }, ".zip", null,
                activityRef.get().getString(R.string.select_patch), false, false, false, "patch", defaultDir);
    }

    private void patchSelected(String filePath) {
        logLayout.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);

        String patchConfig = getPatchConfig(filePath);
        if (patchConfig != null) {
            logTv.setText(patchConfig);
        }

        this.patchPath = filePath;
        this.patchPathTv.setText(patchPath);
        this.selectApplyBtn.setText(R.string.apply_the_patch);
    }

    @Nullable
    private String getPatchConfig(String filePath) {
        ZipFile zfile = null;
        InputStream input = null;
        try {
            zfile = new ZipFile(filePath);
            ZipEntry entry = zfile.getEntry("patch.txt");
            if (entry == null) {
                this.error(R.string.patch_error_no_entry, "patch.txt");
            }

            input = zfile.getInputStream(entry);
            return IOUtils.readString(input);
        } catch (Exception e) {
            error(R.string.general_error, e.getMessage());
        } finally {
            closeQuietly(input);
            closeQuietly(zfile);
        }
        return null;
    }

    @Override
    public void info(int resourceId, boolean bold, Object... args) {
        String txt = activityRef.get().getString(resourceId);
        if (args != null) {
            txt = String.format(txt, args);
        }
        String message = bold ? ("\n" + txt + "\n") : (txt + "\n");
        appendText(message, bold, false);
    }

    @Override
    public void info(String format, boolean bold, Object... args) {
        String txt = format;
        if (args != null) {
            txt = String.format(txt, args);
        }
        String message = bold ? ("\n" + txt + "\n") : (txt + "\n");
        appendText(message, bold, false);
    }

    @Override
    public void error(int resourceId, Object... args) {
        String txt = activityRef.get().getString(resourceId);
        if (args != null) {
            txt = String.format(txt, args);
        }
        appendText(txt + "\n", false, true);
    }

    private void appendText(final String txt, final boolean bold,
                            final boolean red) {
        activityRef.get().runOnUiThread(() -> {
            if (red) {
                SpannableString spanString = new SpannableString(txt);
                ForegroundColorSpan span = new ForegroundColorSpan(
                        Color.RED);
                spanString.setSpan(span, 0, txt.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                logTv.append(spanString);
            } else if (bold) {
                SpannableString spanString = new SpannableString(txt);
                StyleSpan span = new StyleSpan(Typeface.BOLD);
                spanString.setSpan(span, 0, txt.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                logTv.append(spanString);
            } else {
                logTv.append(txt);
            }
        });
    }

    @Override
    public String getString(int stringId) {
        return activityRef.get().getString(stringId);
    }

    @Override
    public void setVariableValue(String key, String value) {
        globalVariableValues.put(key, value);
    }

    @Override
    public String getVariableValue(String key) {
        return globalVariableValues.get(key);
    }

    @Override
    public void patchFinished() {
        activityRef.get().runOnUiThread(() -> {
            selectApplyBtn.setText(R.string.patch_applied);
            selectApplyBtn.setBackgroundColor(0xff606060);
        });
    }

    @Override
    public String getDecodeRootPath() {
        return activityRef.get().getDecodeRootPath();
    }

    @Override
    public List<String> getSmaliFolders() {
        List<String> folders = new ArrayList<>();
        folders.add("smali");

        // Look into zip file to get all dex, thus get related smali folder
        String apkPath = activityRef.get().getApkPath();
        ZipFile zfile = null;
        try {
            zfile = new ZipFile(apkPath);
            Enumeration<? extends ZipEntry> entries = zfile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".dex") && !name.contains("/")) {
                    if (!name.equals("classes.dex")) {
                        folders.add("smali_"
                                + name.substring(0, name.length() - 4));
                    }
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            closeQuietly(zfile);
        }

        return folders;
    }

    private void initManifestInfo() {
        String apkPath = activityRef.get().getApkPath();
        ZipFile zfile = null;
        InputStream input = null;
        try {
            zfile = new ZipFile(apkPath);
            ZipEntry zipEntry = zfile.getEntry("AndroidManifest.xml");
            input = zfile.getInputStream(zipEntry);
            ManifestParser parser = new com.mcal.apkeditor.ce.ManifestParser(
                    input);
            parser.parse();
            this.manifestInfo = parser.getManifestInfo();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(input);
            closeQuietly(zfile);
        }
    }

    @Override
    public String getApplication() {
        if (this.manifestInfo == null) {
            initManifestInfo();
        }
        if (this.manifestInfo != null) {
            String name = manifestInfo.applicationCls;
            if (name == null) {
                return null;
            }
            if (name.startsWith(".")) {
                name = manifestInfo.packageName + name;
            } else if (!name.contains(".")) {
                name = manifestInfo.packageName + "." + name;
            }
            return name;
        }
        return null;
    }

    @Override
    public List<String> getActivities() {
        if (this.manifestInfo == null) {
            initManifestInfo();
        }
        if (this.manifestInfo != null) {
            List<String> activities = new ArrayList<>();
            for (Entry<Integer, Integer> entry : manifestInfo.compNameIdx2Type.entrySet()) {
                // it is activity
                if (entry.getValue() == 0) {
                    String name = getComponentName(manifestInfo, entry.getKey());
                    if (!activities.contains(name)) {
                        activities.add(name);
                    }
                }
            }
            return activities;
        }

        return null;
    }

    @Override
    public List<String> getLauncherActivities() {
        if (this.manifestInfo == null) {
            initManifestInfo();
        }
        if (this.manifestInfo != null) {
            List<String> activities = new ArrayList<>();
            for (Integer idx : manifestInfo.launcherActIdxs) {
                String name = getComponentName(manifestInfo, idx);
                if (!activities.contains(name)) {
                    activities.add(name);
                }
            }
            return activities;
        }

        return null;
    }

    // Get real component name
    @Nullable
    private String getComponentName(@NonNull ManifestInfo manifestInfo, int index) {
        if (manifestInfo.targetActivityIdxs != null
                && manifestInfo.targetActivityIdxs.containsKey(index)) {
            index = manifestInfo.targetActivityIdxs.get(index);
        }

        if (index < manifestInfo.strings.size()) {
            String name = manifestInfo.strings.get(index);
            if (name.startsWith(".")) {
                name = manifestInfo.packageName + name;
            } else if (!name.contains(".")) {
                name = manifestInfo.packageName + "." + name;
            }
            return name;
        }

        return null;
    }

    @Override
    public List<String> getPatchNames() {
        if (patchExecutor != null) {
            return patchExecutor.getRuleNames();
        }
        return null;
    }
}
