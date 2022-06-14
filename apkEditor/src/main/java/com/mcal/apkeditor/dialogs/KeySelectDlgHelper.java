package com.mcal.apkeditor.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.activities.SettingActivity;
import com.mcal.apkeditor.dialogs.FileSelectDialog.IFileSelection;
import com.mcal.apkeditor.view.KeyListPreference;

import java.io.File;
import java.lang.ref.WeakReference;

public class KeySelectDlgHelper implements IFileSelection {
    private final WeakReference<KeyListPreference> preferenceRef;
    private final Context mContext;
    private final EditText pk8PathEt;
    private final EditText x509PathEt;

    public KeySelectDlgHelper(Context context, KeyListPreference p) {
        preferenceRef = new WeakReference<>(p);
        mContext = context;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dlg_keyselect, null, false);
        pk8PathEt = view.findViewById(R.id.et_pk8path);
        x509PathEt = view.findViewById(R.id.et_x509path);
        pk8PathEt.setText(sp.getString(SettingActivity.STR_PRIVATEKEYPATH, ""));
        x509PathEt.setText(sp.getString(SettingActivity.STR_PUBLICKEYPATH, ""));

        ImageButton btn1 = view.findViewById(R.id.btn_select_pk8);
        btn1.setOnClickListener(v -> new FileSelectDialog(mContext, this, ".pk8", ".pk8",
                mContext.getString(R.string.select_key_file)));

        ImageButton btn2 = view.findViewById(R.id.btn_select_x509);
        btn2.setOnClickListener(v -> new FileSelectDialog(mContext, this, ".x509.pem", ".pem",
                mContext.getString(R.string.select_key_file)));

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
        dialog.setView(view);
        dialog.setTitle(R.string.custom_key_setting);
        dialog.setPositiveButton(android.R.string.ok, (v, which) -> setCustomKey());
        dialog.setNegativeButton(android.R.string.cancel, null);
        dialog.show();
    }

    protected void setCustomKey() {
        String privateKeyPath = pk8PathEt.getText().toString();
        String publicKeyPath = x509PathEt.getText().toString();

        // Check file path
        if ("".equals(privateKeyPath) || "".equals(publicKeyPath)) {
            Toast.makeText(mContext, R.string.error_filepath_empty, Toast.LENGTH_LONG).show();
            return;
        } else {
            File f1 = new File(publicKeyPath);
            File f2 = new File(privateKeyPath);
            if (!f1.exists() || !f2.exists()) {
                Toast.makeText(mContext, R.string.error_filepath_notexist, Toast.LENGTH_LONG).show();
                return;
            }
        }

        // To save key file path
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        Editor editor = sp.edit();
        editor.putString(SettingActivity.STR_PRIVATEKEYPATH, privateKeyPath);
        editor.putString(SettingActivity.STR_PUBLICKEYPATH, publicKeyPath);
        editor.apply();

        // Also save the preference value
        preferenceRef.get().setCustomValue();
    }

    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        if (".pk8".equals(extraStr)) {
            pk8PathEt.setText(filePath);
        } else {
            x509PathEt.setText(filePath);
        }
    }

    @Override
    public boolean isInterestedFile(@NonNull String filename, String extraStr) {
        return (filename.endsWith(".pk8") || filename.endsWith(extraStr));
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }
}
