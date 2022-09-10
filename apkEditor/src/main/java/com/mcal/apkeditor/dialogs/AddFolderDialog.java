package com.mcal.apkeditor.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.mcal.apkeditor.R;
import com.mcal.common.utils.InputHelper;

import java.io.File;
import java.lang.ref.WeakReference;

// Create a new directory or import a directory
public class AddFolderDialog implements View.OnClickListener, FileSelectDialog.IFileSelection {
    private final AddFolderCallback mCallback;
    private final WeakReference<Context> contextRef;
    private final View newDivider;
    private final View importDivider;
    private final View newFolderLayout;
    private final View importFolderLayout;
    private final TextInputEditText folderNameEt;

    // Callback functions for folder selection
    private final TextInputEditText folderPathEt;
    private final AlertDialog materialDialog;
    private boolean addFolder = true;

    public AddFolderDialog(final Context context, AddFolderCallback callback, boolean showImportFolder) {
        contextRef = new WeakReference<>(context);
        mCallback = callback;

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_folder, null);
        TextView newTv = (TextView) view.findViewById(R.id.tv_new_folder);
        TextView importTv = (TextView) view.findViewById(R.id.tv_import_folder);
        if (!showImportFolder) {
            importTv.setVisibility(View.GONE);
        }

        newDivider = view.findViewById(R.id.divider1);
        importDivider = view.findViewById(R.id.divider2);
        newFolderLayout = view.findViewById(R.id.layout_new);
        importFolderLayout = view.findViewById(R.id.layout_import);

        folderNameEt = (TextInputEditText) view.findViewById(R.id.et_folder_name);
        folderPathEt = (TextInputEditText) view.findViewById(R.id.et_folder_path);

        InputFilter filter = InputHelper.getFileNameFilter();
        folderNameEt.setFilters(new InputFilter[]{filter});

        newTv.setOnClickListener(this);
        importTv.setOnClickListener(this);

        materialDialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    confirm();
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton("Select", null)
                .create();

        materialDialog.setOnShowListener(dialogShowListener -> {
            materialDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> {
                browse();
                materialDialog.dismiss();
            });
            materialDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.GONE);
        });
        materialDialog.show();
    }

    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        folderPathEt.setText(filePath);
    }

    @Override
    public boolean isInterestedFile(String filename, String extraStr) {
        return false;
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.tv_new_folder) {
            showNewFolderView();
        } else if (id == R.id.tv_import_folder) {
            showImportFolderView();
        }
    }

    // Browse SD card to select a folder
    private void browse() {
        Context ctx = contextRef.get();
        String strTitle = ctx.getString(R.string.select_imported_folder);
        new FileSelectDialog(ctx, this,
                "", "", strTitle,
                true, false, false,
                "import_folder");
    }

    // Confirm to create or import a directory
    private void confirm() {
        if (addFolder) {
            String name = folderNameEt.getText().toString();
            name = name.trim();
            if ("".equals(name)) {
                Toast.makeText(contextRef.get(), R.string.empty_input_tip, Toast.LENGTH_LONG).show();
            } else {
                mCallback.addFolder(name);
                materialDialog.dismiss();
            }
        }
        // To import a folder
        else {
            String path = folderPathEt.getText().toString();
            path = path.trim();
            if ("".equals(path)) {
                Toast.makeText(contextRef.get(), R.string.empty_input_tip, Toast.LENGTH_LONG).show();
            } else if (!new File(path).exists()) {
                String fmt = contextRef.get().getString(R.string.error_path_xxx_not_exist);
                String message = String.format(fmt, path);
                Toast.makeText(contextRef.get(), message, Toast.LENGTH_LONG).show();
            } else {
                mCallback.importFolder(path);
                materialDialog.dismiss();
            }
        }
    }

    private void showNewFolderView() {
        addFolder = true;
        newDivider.setVisibility(View.VISIBLE);
        importDivider.setVisibility(View.INVISIBLE);
        newFolderLayout.setVisibility(View.VISIBLE);
        importFolderLayout.setVisibility(View.INVISIBLE);
        materialDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
        materialDialog.show();
    }

    private void showImportFolderView() {
        addFolder = false;
        newDivider.setVisibility(View.INVISIBLE);
        importDivider.setVisibility(View.VISIBLE);
        newFolderLayout.setVisibility(View.INVISIBLE);
        importFolderLayout.setVisibility(View.VISIBLE);
        materialDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
        materialDialog.show();
    }

    public interface AddFolderCallback {
        void addFolder(String folderName);

        void importFolder(String folderPath);
    }
}

