package com.mcal.apkeditor.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.view.ViewDialog;
import com.mcal.common.utils.ClipboardUtil;

import java.lang.ref.WeakReference;

public class DebugDialog extends ViewDialog implements View.OnClickListener {
    private final EditText logEt;
    private final Button closeBtn;

    private final WeakReference<Context> contextRef;
    private String strLog = "";

    public DebugDialog(Context context) {
        super(context);

        this.contextRef = new WeakReference<>(context);

        View layout = LayoutInflater.from(context).inflate(R.layout.dialog_debug, null);
        this.logEt = (EditText) layout.findViewById(R.id.et_log);
        this.closeBtn = (Button) layout.findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);
        layout.findViewById(R.id.btn_copy).setOnClickListener(this);

        this.setContentView(layout);
    }

    public void addLog(String line) {
        this.strLog += line + "\n";
        logEt.setText(strLog);
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.btn_copy) {
            ClipboardUtil.copyToClipboard(contextRef.get(), strLog);
        } else if (id == R.id.btn_close) {
            this.dismiss();
        }
    }
}
