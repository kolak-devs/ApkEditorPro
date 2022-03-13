package com.gmail.heagoo.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.gmail.heagoo.apkeditor.StringListAdapter;
import com.gmail.heagoo.apkeditor.R;
import com.mcal.common.utils.ClipboardUtil;

import java.lang.ref.WeakReference;

import ru.svolf.melissa.sheet.ViewDialog;

public class StringValueDialog implements android.view.View.OnClickListener {

    private final WeakReference<Context> ctxRef;
    private final StringListAdapter strListAdapter;
    private final int position;

    private final View view;
    private final TextView keyTv;
    private final EditText valueEt;
    private final ViewDialog dialog;

    @SuppressLint("InflateParams")
    public StringValueDialog(Context context, StringListAdapter strListAdapter,
                             int position) {

        this.ctxRef = new WeakReference<>(context);
        this.strListAdapter = strListAdapter;
        this.position = position;

        this.view = LayoutInflater.from(context).inflate(R.layout.dlg_stringvalue, null);

        dialog = new ViewDialog(context);
        dialog.setTitle(R.string.edit_string_value);
        dialog.setView(view);

        // getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        this.keyTv = view.findViewById(R.id.key);
        this.valueEt = view.findViewById(R.id.value);
        View menu = view.findViewById(R.id.menu_clipboard);
        menu.setOnClickListener(this);

        Button okBtn = view.findViewById(R.id.btn_editstring_ok);
        okBtn.setOnClickListener(this);

        Button cancelBtn = view
                .findViewById(R.id.btn_editstring_cancel);
        cancelBtn.setOnClickListener(this);
    }

    public void show() {
        dialog.show();
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.btn_editstring_ok) {
            String newValue = valueEt.getText().toString();
            strListAdapter.checkTextChange(position, newValue);
            dialog.dismiss();
        } else if (id == R.id.btn_editstring_cancel) {
            dialog.cancel();
        } else if (id == R.id.menu_clipboard) {
            Context ctx = ctxRef.get();
            String str = keyTv.getText().toString();
            ClipboardUtil.copyToClipboard(ctx, str);

            String msg = ctx.getString(R.string.copied_to_clipboard);
            msg = String.format(msg, str);
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
        }
    }

    public void setKeyValue(String key, String val) {
        keyTv.setText(key);
        valueEt.setText(val);
        valueEt.setSelection(val != null ? val.length() : 0);
    }
}
