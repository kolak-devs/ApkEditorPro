package com.mcal.apkeditor.data;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.BuildConfig;
import com.mcal.apkeditor.R;

public class Dialogs {
    public static void about(Context context) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(40, 0, 40, 0);
        ll.setLayoutParams(layoutParams);
        AppCompatTextView msg = new AppCompatTextView(context);

        try {
            PackageInfo pInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            msg.setText("\nVersion: " + pInfo.versionName + "\n\nSmali: " + BuildConfig.SMALI_VERSION + "\nJaDX: " + BuildConfig.JADX_VERSION + "\nApkTool: " +  BuildConfig.APKTOOL_VERSION + "\nAndroid API: v32\n\nCreated by Russian with ❤️\n© Copyright 2021-2022 Тимашков Иван");
        } catch (Exception e) {
            e.printStackTrace();
        }

        ll.addView(msg);

        final MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setTitle(R.string.app_name);
        dialog.setView(ll);
        dialog.setPositiveButton("OK", null);
        dialog.show();
    }
}
