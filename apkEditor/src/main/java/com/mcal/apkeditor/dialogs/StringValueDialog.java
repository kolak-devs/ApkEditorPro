package com.mcal.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.StringListAdapter;
import com.mcal.common.utils.ClipboardUtils;

public class StringValueDialog {
    private final TextView key;
    private final EditText value;

    @SuppressLint("InflateParams")
    public StringValueDialog(Context context, StringListAdapter strListAdapter, int position) {
        final Context mContext = context;

        View view = LayoutInflater.from(mContext).inflate(R.layout.dlg_stringvalue, null);
        ImageButton menu = view.findViewById(R.id.menu_clipboard);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = key.getText().toString();
                ClipboardUtils.copyToClipboard(mContext, str);
                String msg = mContext.getString(R.string.copied_to_clipboard);
                msg = String.format(msg, str);
                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
            }
        });
        key = view.findViewById(R.id.key);
        value = view.findViewById(R.id.value);

        AlertDialog materialDialog = new MaterialAlertDialogBuilder(mContext)
                .setView(view)
                .create();
        materialDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(android.R.string.ok), (dialog, which) -> {
            String newValue = value.getText().toString();
            strListAdapter.checkTextChange(position, newValue);
            dialog.dismiss();
        });
        materialDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss());
        materialDialog.show();
    }

    public void setKeyValue(String key, String val) {
        this.key.setText(key);
        value.setText(val);
        value.setSelection(val != null ? val.length() : 0);
    }
}
