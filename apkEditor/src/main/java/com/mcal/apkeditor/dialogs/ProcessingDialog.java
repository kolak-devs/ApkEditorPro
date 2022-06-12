package com.mcal.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.R;

import java.lang.ref.WeakReference;

public class ProcessingDialog extends Dialog implements
        android.view.View.OnClickListener {

    private final WeakReference<Activity> activityRef;
    private final ProcessingInterface processor;
    private final int successTipResId;

    @SuppressLint("InflateParams")
    public ProcessingDialog(Activity activity, ProcessingInterface processor,
                            int okTipResId) {
        super(activity);
        this.activityRef = new WeakReference<>(activity);
        this.processor = processor;
        this.successTipResId = okTipResId;

        LayoutInflater inflater = LayoutInflater.from(activity);
        View layout = inflater.inflate(R.layout.dlg_processing, null);
        setContentView(layout);
        setCancelable(false);

        // Start processing thread
        ProcessingThread thread = new ProcessingThread(this);
        thread.start();
    }

    // When errMsg == null, means revert succeed
    protected void processCompleted(final String errMsg) {
        Activity activity = activityRef.get();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                // Call back to the task which is mainly for UI change
                processor.afterProcess();

                if (errMsg != null) {
                    showTip("Failed: " + errMsg);
                } else {
                    showTip(successTipResId);
                }

                if (ProcessingDialog.this.isShowing()) {
                    dismissWithoutThrow();
                }
            });
        }
    }

    // Don't know why occurred, but it appears on google play
    private void dismissWithoutThrow() {
        try {
            dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void showTip(String msg) {
        Activity activity = activityRef.get();
        if (activity != null) {
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
        }
    }

    protected void showTip(int resId) {
        if (resId != -1) {
            Activity activity = activityRef.get();
            if (activity != null) {
                Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.close_button) {
            dismissWithoutThrow();
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
            String errMsg = null;

            ProcessingDialog dlg = dlgRef.get();
            if (dlg != null) {
                try {
                    dlg.processor.process();
                } catch (Exception e) {
                    errMsg = e.getMessage();
                }
                dlg.processCompleted(errMsg);
            }
        }
    }
}
