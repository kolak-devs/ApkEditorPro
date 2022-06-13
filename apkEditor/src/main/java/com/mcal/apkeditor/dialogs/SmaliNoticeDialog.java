package com.mcal.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.R;
import com.mcal.common.utils.PreferenceUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SmaliNoticeDialog {
    private final View view;
    private final Context mContext;

    @SuppressLint("InflateParams")
    public SmaliNoticeDialog(Context context) {
        mContext = context;
        view = LayoutInflater.from(context).inflate(R.layout.dlg_smali_license, null);

        AlertDialog materialDialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .create();

        InputStream in = null;
        StringBuilder sb = new StringBuilder();
        try {
            in = mContext.getAssets().open("smali-NOTICE");
            InputStreamReader reader = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(reader);
            String line = br.readLine();
            while (line != null) {
                sb.append(line).append("\n");
                line = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        TextView tv = view.findViewById(R.id.content);
        tv.setText(sb.toString());

        materialDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(android.R.string.ok), (dialog, which) -> {
            CheckBox cb = view.findViewById(R.id.cb_show_once);
            if (cb.isChecked()) {
                PreferenceUtils.setBoolean(mContext, "smali_license_showed", true);
            }
            dialog.dismiss();
        });
        materialDialog.show();
    }
}
