package com.mcal.apksigner.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;

import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.mcal.apksigner.R;
import com.mcal.common.data.Preferences;
import com.mcal.common.utils.ScopedStorage;

import org.jetbrains.annotations.Contract;

import java.io.File;

public class CustomSignDialog2 {
    private final Context context;
    private final DialogInterface.OnClickListener listener;
    private FilePickerDialog pk8Dialog;
    private FilePickerDialog x509Dialog;

    @Contract(pure = true)
    public CustomSignDialog2(Context context, DialogInterface.OnClickListener listener) {
        this.listener = listener;
        this.context = context;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.dialog_keystore_pk8_pem, null);

        AppCompatEditText pk8Path = view.findViewById(R.id.pk8);
        AppCompatEditText x509Path = view.findViewById(R.id.x509);

        AppCompatImageButton btnPk8 = view.findViewById(R.id.pk8_path);

        AppCompatImageButton btnX509 = view.findViewById(R.id.x509_path);

        pk8Path.setText(Preferences.getPk8());
        pk8Path.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {
            }

            @Override
            public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {
            }

            @Override
            public void afterTextChanged(Editable p1) {
                Preferences.setPk8(p1.toString());
            }
        });

        x509Path.setText(Preferences.getX509());
        x509Path.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {
            }

            @Override
            public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {
            }

            @Override
            public void afterTextChanged(Editable p1) {
                Preferences.setX509(p1.toString());
            }
        });

        pk8Dialog = new FilePickerDialog(context, pk8Properties());
        pk8Dialog.setTitle(context.getString(R.string.select));
        pk8Dialog.setPositiveBtnName(context.getString(R.string.select));
        pk8Dialog.setNegativeBtnName(context.getString(android.R.string.cancel));

        x509Dialog = new FilePickerDialog(context, x509Properties());
        x509Dialog.setTitle(context.getString(R.string.select_keystore));
        x509Dialog.setPositiveBtnName(context.getString(R.string.select));
        x509Dialog.setNegativeBtnName(context.getString(android.R.string.cancel));

        final AlertDialog dialog = builder.setTitle(R.string.custom_keystore)
                .setPositiveButton(R.string.save, listener)
                .setNegativeButton(android.R.string.cancel, null)
                .setView(view)
                .create();


        btnPk8.setOnClickListener(v -> pk8Dialog.show());

        btnX509.setOnClickListener(v -> x509Dialog.show());

        pk8Dialog.setDialogSelectionListener(files -> {
            for (String path : files) {
                File file = new File(path);
                pk8Path.setText(file.getAbsolutePath());
                pk8Dialog.dismiss();
                dialog.show();
            }
        });

        x509Dialog.setDialogSelectionListener(files -> {
            for (String path : files) {
                File file = new File(path);
                x509Path.setText(file.getAbsolutePath());
                x509Dialog.dismiss();
                dialog.show();
            }
        });
        dialog.show();
    }

    public DialogProperties pk8Properties() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(ScopedStorage.getStorageDirectory().getAbsolutePath());
        properties.extensions = new String[]{".pk8", ".PK8"};
        return properties;
    }

    public DialogProperties x509Properties() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(ScopedStorage.getStorageDirectory().getAbsolutePath());
        properties.extensions = new String[]{".pem", ".PEM", ".x509.pem", ".X509.PEM"};
        return properties;
    }
}