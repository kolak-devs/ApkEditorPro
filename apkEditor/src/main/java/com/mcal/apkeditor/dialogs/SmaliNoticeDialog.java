package com.mcal.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.view.ViewDialog;
import com.mcal.common.utils.PreferenceUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SmaliNoticeDialog implements
        android.view.View.OnClickListener {

    private final View view;
    private final Context context;
    private final ViewDialog dialog;

    @SuppressLint("InflateParams")
    public SmaliNoticeDialog(Context context) {
        this.context = context;

        this.view = LayoutInflater.from(context).inflate(R.layout.dlg_smali_license, null);

        dialog = new ViewDialog(context);
        dialog.setTitle(R.string.notice_capital);
        dialog.setView(view);

        initView();
    }

    public void show() {
        dialog.show();
    }

    private void initView() {
        InputStream in = null;
        StringBuilder sb = new StringBuilder();
        try {
            in = context.getAssets().open("smali-NOTICE");
            InputStreamReader reader = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(reader);
            String line = br.readLine();
            while (line != null) {
                sb.append(line + "\n");
                line = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

        TextView tv = (TextView) view.findViewById(R.id.content);
        tv.setText(sb.toString());

        Button closeBtn = (Button) view.findViewById(R.id.close_button);
        closeBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        CheckBox cb = (CheckBox) view.findViewById(R.id.cb_show_once);
        if (cb.isChecked()) {
            PreferenceUtils.setBoolean(context, "smali_license_showed", true);
        }
        dialog.dismiss();
    }
}
