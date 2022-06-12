package com.mcal.apkeditor.dialogs;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.R;

public class LinesOpDialogHelper {

    private Context context;
    private ILinesOperation opInterface;

    private EditText fromEt;
    private EditText toEt;

    private AlertDialog d;

    public void showDialog(Context context, int titleResId, ILinesOperation opInterface) {
        this.context = context;
        this.opInterface = opInterface;

        // Create view
        LayoutInflater inflater = LayoutInflater.from(context);
        final ViewGroup nullParent = null;
        View content = inflater.inflate(R.layout.dlg_lines_op, nullParent);
        initView(content);

        this.d = new MaterialAlertDialogBuilder(context)
                .setView(content)
                .setTitle(titleResId)
                .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        d.setOnShowListener(dialog -> {

            Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
            b.setOnClickListener(view -> positiveBtnClicked());
        });

        this.d.show();
    }

    private void positiveBtnClicked() {
        String fromStr = fromEt.getText().toString();
        String toStr = toEt.getText().toString();
        if ("".equals(fromStr)) {
            Toast.makeText(context, R.string.empty_input_tip, Toast.LENGTH_LONG).show();
            fromEt.requestFocus();
            return;
        }
        if ("".equals(toStr)) {
            Toast.makeText(context, R.string.empty_input_tip, Toast.LENGTH_LONG).show();
            toEt.requestFocus();
            return;
        }
        try {
            int from = Integer.parseInt(fromStr);
            int to = Integer.parseInt(toStr);
            if (from > to) {
                Toast.makeText(context, R.string.err_from_greater_than_to, Toast.LENGTH_LONG).show();
                fromEt.requestFocus();
                return;
            }
            if (opInterface != null) {
                opInterface.operateLines(from, to);
                d.dismiss();
            }
        } catch (Exception e) {
            Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_LONG).show();
        }
    }

    private void initView(@NonNull View content) {
        this.fromEt = (EditText) content.findViewById(R.id.et_from);
        this.toEt = (EditText) content.findViewById(R.id.et_to);
    }

    public interface ILinesOperation {
        void operateLines(int from, int to);
    }
}
