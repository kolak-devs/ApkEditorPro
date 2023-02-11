package com.mcal.appdm;

import static com.mcal.common.utils.FileHelperKt.copyFile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.mcal.appdm.base.R;
import com.mcal.common.utils.FileHelperKt;
import com.mcal.common.utils.ScopedStorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ApkSaveDialog extends Dialog {
    private final Activity mActivity;
    private final String mApkPath;
    private final String mAppName;
    private String dstPath;

    @SuppressLint("InflateParams")
    public ApkSaveDialog(Activity activity, String apkPath, String appName) {
        super(activity);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCancelable(false);
        mActivity = activity;
        mApkPath = apkPath;
        mAppName = appName;
        View view = LayoutInflater.from(activity).inflate(R.layout.appdm_dlg_saveapk, null);
        setContentView(view);
    }

    public void start() {
        show();
        if (!FileHelperKt.exist()) {
            showToast("Cannot find SD card to save the APK.");
            return;
        }
        dstPath = ScopedStorage.getBackupsDir() + File.separator + mAppName + ".apk";
        startCopyThread(mApkPath, dstPath);
    }

    private void startCopyThread(final String srcPath, final String dstPath) {
        new Thread() {
            @Override
            public void run() {
                try {
                    FileInputStream in = new FileInputStream(srcPath);
                    FileOutputStream out = new FileOutputStream(dstPath);
                    copyFile(in, out);
                    onSucceed();
                } catch (IOException e) {
                    onFailed(e.getMessage());
                }
            }
        }.start();
    }

    protected void onSucceed() {
        String str = mActivity.getResources().getString(R.string.apk_saved_tip);
        showToastOnUiThread(String.format(str, dstPath));
        cancelDialog();
    }

    protected void onFailed(String msg) {
        showToastOnUiThread("Failed: " + msg);
        cancelDialog();
    }

    private void cancelDialog() {
        final Activity activity = mActivity;
        if (activity != null) {
            activity.runOnUiThread(this::cancel);
        }
    }

    private void showToast(String msg) {
        Activity activity = mActivity;
        if (activity != null) {
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void showToastOnUiThread(final String msg) {
        final Activity activity = mActivity;
        if (activity != null) {
            activity.runOnUiThread(() -> Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show());
        }
    }
}