package com.mcal.apkeditor.activities;


import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.ApkComposeThread;
import com.mcal.apkeditor.BuildConfig;
import com.mcal.apkeditor.GlobalConfig;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.dialogs.ProcessingDialog;
import com.mcal.apkeditor.se.ApkCreateActivity;
import com.mcal.apkeditor.se.IDirChanged;
import com.mcal.apkeditor.se.ZipFileListAdapter;
import com.mcal.apkeditor.se.ZipHelper;
import com.mcal.common.activities.CustomizedLangActivity;
import com.mcal.common.utils.ActivityUtils;
import com.mcal.common.utils.ApkInfoParser;
import com.mcal.common.utils.CommandRunner;
import com.mcal.common.utils.RandomUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;

public class AxmlEditActivity extends CustomizedLangActivity implements IDirChanged, View.OnClickListener {
    private String apkPath;
    private ApkInfoParser.AppInfo apkInfo;

    private int themeId;
    private ZipFileListAdapter filesAdapter;
    // Thread & handler
    private AxmlEditActivity.MyHandler handler;
    private AxmlEditActivity.MyThread thread;
    // Zip file list view
    private ListView fileListView;
    // Save/Close Button
    private Button saveBtn;
    // Summary text (to show tip)
    private TextView summaryTv;
    // To parse all the information inside the APK
    private ZipHelper zipHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_axmledit);

        // Full screen or not
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        this.apkPath = ActivityUtils.getParam(getIntent(), "apkPath");

        try {
            this.apkInfo = new ApkInfoParser().parse(this, apkPath);
        } catch (Exception e) {
            String msg = getResources().getString(R.string.cannot_parse_apk);
            msg += ": " + e.getMessage();
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }

        if (apkInfo != null) {
            // Start thread to parse the apk file
            this.handler = new AxmlEditActivity.MyHandler(this);
            this.thread = new AxmlEditActivity.MyThread(this);
            thread.start();

            initViews();
        } else {
            this.finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if (filesAdapter != null) {
            filesAdapter.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            // XML file is modified
            if (resultCode == 1) {
                String filePath = data.getStringExtra("xmlPath");
                String entryName = data.getStringExtra("extraString");
                new ProcessingDialog(this, new XmlCompiler(filePath, entryName), -1);
            }
        }
    }

    @Nullable
    private String getApkPath() {
        String packageName = (BuildConfig.IS_PRO ? "com.mcal.apkeditor.pro" : "com.mcal.apkeditor");
        PackageManager pm = this.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            String apk = ai.publicSourceDir;
            return apk;
        } catch (Throwable x) {
        }
        return null;
    }

    @NonNull
    private String getBinaryPath() {
        // Play tricks to extract files: borrow ApkComposeThread to extract files
        ApkComposeThread tmp = new ApkComposeThread(this, null, null, null);
        tmp.prepare();

        File fileDir = this.getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();

        return rootDirectory + "/bin/";
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save APK path
        {
            outState.putString("apkPath", this.apkPath);
        }

        super.onSaveInstanceState(outState);
    }

    public void dataReady(boolean succeed) {
        // Make progress bar gone
        this.findViewById(R.id.progress_bar).setVisibility(View.GONE);

        if (succeed) {
            fileListView.setVisibility(View.VISIBLE);
            initCenterView();
        } else {
            Toast.makeText(this, thread.getErrorMessage(), Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    private void initCenterView() {
        // Files
        this.filesAdapter = new ZipFileListAdapter(this, this, zipHelper, true);
        fileListView.setAdapter(filesAdapter);
        fileListView.setOnItemClickListener(filesAdapter);
        fileListView.setOnItemLongClickListener(filesAdapter);
    }

    private void initViews() {
        // Get View
        this.fileListView = findViewById(R.id.files_list);
        this.summaryTv = this.findViewById(R.id.tv_summary);
        this.saveBtn = this.findViewById(R.id.btn_save);
        Button closeBtn = this.findViewById(R.id.btn_close);

        // Set the list view invisible
        fileListView.setVisibility(View.INVISIBLE);

        this.saveBtn.setOnClickListener(this);
        closeBtn.setOnClickListener(this);

        // Basic info
        if (apkInfo != null) {
            ImageView apkIcon = this.findViewById(R.id.apk_icon);
            apkIcon.setImageDrawable(apkInfo.icon);

            TextView labelTV = this.findViewById(R.id.apk_label);
            labelTV.setText(apkInfo.label);
        }
    }

    private void initData() throws Exception {
        this.zipHelper = new ZipHelper(this.apkPath);
        this.zipHelper.parse();
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            this.finish();
        } else if (id == R.id.btn_save) {
            makeAPK();
            this.finish();
        }
    }

    // To make the new modified APK
    private void makeAPK() {
        Map<String, String> fileReplaces = filesAdapter.getReplaces();

        Intent intent = new Intent(this, ApkCreateActivity.class);
        ActivityUtils.attachParam(intent, "apkPath", this.apkPath);
        ActivityUtils.attachParam(intent, "packageName", apkInfo.packageName);
        if (!fileReplaces.isEmpty()) {
            ActivityUtils.attachParam(intent, "otherReplaces", fileReplaces);
        }

        startActivity(intent);
    }

    @Override
    public void dirChanged(String dir) {
        this.summaryTv.setText(dir);
    }

    private static class MyHandler extends Handler {
        WeakReference<AxmlEditActivity> activityRef;

        public MyHandler(AxmlEditActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            AxmlEditActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                case 0:
                    activity.dataReady(true);
                    break;
                case 1:
                    activity.dataReady(false);
                    break;
            }
        }
    }

    private static class MyThread extends Thread {
        String err;
        WeakReference<AxmlEditActivity> activityRef;

        public MyThread(AxmlEditActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            AxmlEditActivity activity = activityRef.get();
            if (activity != null) {
                try {
                    activity.initData();
                    activity.handler.sendEmptyMessage(0);
                } catch (Exception e) {
                    e.printStackTrace();
                    err = e.getMessage();
                    activity.handler.sendEmptyMessage(1);
                }
            }
        }

        String getErrorMessage() {
            return err;
        }
    }

    // Compile xml into AXML
    private class XmlCompiler implements ProcessingDialog.ProcessingInterface {
        private final String xmlPath;
        private final String axmlPath;
        private final String tempPath;
        private final String entryName;
        private String outMessage;
        private String errMessage;
        private boolean succeed = false;

        public XmlCompiler(String filePath, String entryName) {
            this.xmlPath = filePath;
            this.axmlPath = filePath + ".bin";
            this.entryName = entryName;
            this.tempPath = filePath + RandomUtils.getRandomString(6);
        }

        @Override
        public void process() throws Exception {
            String binaryPath = getBinaryPath();
            String aaptPath = binaryPath + "aaptz";
            String androidPath = binaryPath + "android-framework.jar";
            CommandRunner cr = new CommandRunner();
            cr.runCommand(aaptPath + " z -I " + androidPath + " " + xmlPath + " " + tempPath + " " + getApkPath(), null, 5000, false);
            this.outMessage = cr.getStdOut();
            this.errMessage = cr.getStdError();

            // Rename the generated axml
            File generated = new File(tempPath);
            if (generated.exists()) {
                succeed = true;
                // Rename to target file
                File targetFile = new File(axmlPath);
                boolean ret = generated.renameTo(targetFile);
                if (!ret) {
                    generated.delete();
                }
            }
        }

        @Override
        public void afterProcess() {
            if (succeed && new File(axmlPath).exists()) {
                String str = String.format(getString(R.string.entry_modified), entryName);
                Toast.makeText(AxmlEditActivity.this, str, Toast.LENGTH_SHORT).show();

                filesAdapter.addReplace(entryName, axmlPath);
                saveBtn.setVisibility(View.VISIBLE);
            } else {
                String message = outMessage;
                if (errMessage != null && !errMessage.equals("")) {
                    message = errMessage;
                }
                new MaterialAlertDialogBuilder(AxmlEditActivity.this)
                        .setTitle(R.string.error)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }
    }
}
