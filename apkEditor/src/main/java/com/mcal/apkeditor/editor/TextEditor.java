package com.mcal.apkeditor.editor;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.activities.SettingEditorActivity;
import com.mcal.apkeditor.activities.TextEditBigActivity;
import com.mcal.apkeditor.activities.TextEditNormalActivity;
import com.mcal.common.utils.ActivityUtils;

import java.io.File;
import java.util.ArrayList;

public class TextEditor {
    private static boolean isBigFile(Context ctx, String filepath) {
        boolean isBigFile = false;
        File f = new File(filepath);
        if (f.exists() && f.length() > SettingEditorActivity.getBigFileThreshold(ctx) * 1024L) {
            isBigFile = true;
        }
        if (isBigFile) {
            Toast.makeText(ctx, R.string.use_bfe_tip, Toast.LENGTH_SHORT).show();
        }
        return isBigFile;
    }


    @NonNull
    public static Intent getEditorIntent(Context ctx, String filepath, String apkPath) {
        Intent intent;
        if (isBigFile(ctx, filepath)) {
            intent = new Intent(ctx, TextEditBigActivity.class);
        } else {
            intent = new Intent(ctx, TextEditNormalActivity.class);
        }
        ActivityUtils.attachParam(intent, "xmlPath", filepath);
        if (apkPath != null) {
            ActivityUtils.attachParam(intent, "apkPath", apkPath);
        }
        return intent;
    }

    @NonNull
    public static Intent getEditorIntent(Context ctx, @NonNull ArrayList<String> filePathList, int index, String apkPath) {
        Intent intent;
        if (isBigFile(ctx, filePathList.get(index))) {
            intent = new Intent(ctx, TextEditBigActivity.class);
        } else {
            intent = new Intent(ctx, TextEditNormalActivity.class);
        }
        ActivityUtils.attachParam(intent, "fileList", filePathList);
        ActivityUtils.attachParam(intent, "curFileIndex", index);
        if (apkPath != null) {
            ActivityUtils.attachParam(intent, "apkPath", apkPath);
        }
        return intent;
    }
}