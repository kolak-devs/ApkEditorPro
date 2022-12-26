package com.mcal.apkeditor.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.mcal.apkeditor.R;

// Create a new directory or import a directory
public class AddFolderDialog {

    public AddFolderDialog(final Context context, AddFolderCallback callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_folder, null);

        TextInputEditText textView = view.findViewById(R.id.file_name);

        MaterialAlertDialogBuilder materialDialog = new MaterialAlertDialogBuilder(context);
        materialDialog.setView(view);
        materialDialog.setPositiveButton("Folder", (dialog, which) -> {
            final String text = textView.getText().toString();
            if (text.length() > 0) {
                callback.addFolder(text);
                dialog.dismiss();
            }
        });
        materialDialog.setNegativeButton("File", (dialog, which) -> {
            final String text = textView.getText().toString();
            if (text.length() > 0) {
                callback.addFile(text);
                dialog.dismiss();
            }
        });
        materialDialog.setNeutralButton("Import", (dialog, which) -> {
            final String text = textView.getText().toString();
            if (text.length() > 0) {
                callback.importFile(text);
                dialog.dismiss();
            }
        });
        materialDialog.show();
    }


    public interface AddFolderCallback {
        void addFolder(String folderName);

        void addFile(String folderName);

        void importFile(String folderPath);
    }
}

