package com.mcal.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.R;

import java.lang.ref.WeakReference;

public class ProcessingDialog {
    private final Activity mActivity;
    private final ProcessingInterface mProcessor;
    private final int successTipResId;
    private final AlertDialog materialDialog;

    @SuppressLint("InflateParams")
    public ProcessingDialog(Activity activity, ProcessingInterface processor,
                            int okTipResId) {
        mActivity = activity;
        mProcessor = processor;
        successTipResId = okTipResId;

        LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(R.layout.dlg_processing, null);

        materialDialog = new MaterialAlertDialogBuilder(activity)
                .setView(view)
                .setCancelable(false)
                .create();
        materialDialog.show();

        // Start processing thread
        ProcessingThread thread = new ProcessingThread(this);
        thread.start();
    }

    // When errMsg == null, means revert succeed
    protected void processCompleted(final String errMsg) {
        final Activity activity = mActivity;
        if (activity != null) {
            activity.runOnUiThread(() -> {
                // Call back to the task which is mainly for UI change
                mProcessor.afterProcess();
                if (errMsg != null) {
                    showTip("Failed: " + errMsg);
                } else {
                    showTip(successTipResId);
                }
                if (materialDialog.isShowing()) {
                    materialDialog.dismiss();
                }
            });
        }
    }

    protected void showTip(String msg) {
        final Activity activity = mActivity;
        if (activity != null) {
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
        }
    }

    protected void showTip(int resId) {
        if (resId != -1) {
            final Activity activity = mActivity;
            if (activity != null) {
                Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public interface ProcessingInterface {
        void process() throws Exception;

        void afterProcess();
    }

    static class ProcessingThread extends Thread {
        private final WeakReference<ProcessingDialog> dlgRef;

        public ProcessingThread(ProcessingDialog dlg) {
            this.dlgRef = new WeakReference<>(dlg);
        }

        @Override
        public void run() {
            ProcessingDialog dlg = dlgRef.get();
            if (dlg != null) {
                try {
                    dlg.mProcessor.process();
                } catch (Exception e) {
                    dlg.processCompleted(e.getMessage());
                }
            }
        }
    }
}