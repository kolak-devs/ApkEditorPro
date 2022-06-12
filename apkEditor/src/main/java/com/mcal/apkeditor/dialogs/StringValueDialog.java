package com.mcal.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.StringListAdapter;
import com.mcal.common.utils.ClipboardUtils;

import java.lang.ref.WeakReference;

public class StringValueDialog extends Dialog implements android.view.View.OnClickListener {

    private final WeakReference<Context> ctxRef;
    private final StringListAdapter strListAdapter;
    private final int position;

    private final View view;
    private final TextView keyTv;
    private final EditText valueEt;

    @SuppressLint("InflateParams")
    public StringValueDialog(Context context, StringListAdapter strListAdapter,
                             int position) {
        super(context);

        this.ctxRef = new WeakReference<>(context);
        this.strListAdapter = strListAdapter;
        this.position = position;

        this.view = LayoutInflater.from(context).inflate(R.layout.dlg_stringvalue, null);

        setTitle(R.string.edit_string_value);
        setContentView(view);

        // getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        this.keyTv = view.findViewById(R.id.key);
        this.valueEt = view.findViewById(R.id.value);
        AppCompatImageButton menu = view.findViewById(R.id.menu_clipboard);
        menu.setOnClickListener(this);

        Button okBtn = view.findViewById(R.id.btn_editstring_ok);
        okBtn.setOnClickListener(this);

        Button cancelBtn = view
                .findViewById(R.id.btn_editstring_cancel);
        cancelBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.btn_editstring_ok) {
            String newValue = valueEt.getText().toString();
            strListAdapter.checkTextChange(position, newValue);
            dismiss();
        } else if (id == R.id.btn_editstring_cancel) {
            cancel();
        } else if (id == R.id.menu_clipboard) {
            Context ctx = ctxRef.get();
            String str = keyTv.getText().toString();
            ClipboardUtils.copyToClipboard(ctx, str);

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
