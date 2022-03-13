package com.gmail.heagoo.apkeditor.ui;

import android.content.Context;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.gmail.heagoo.apkeditor.R;
import com.gmail.heagoo.apkeditor.dialogs.FileSelectDialog;
import com.mcal.common.utils.InputUtil;

import java.io.File;
import java.lang.ref.WeakReference;

import ru.svolf.melissa.sheet.ViewDialog;


// Create a new directory or import a directory
public class AddFolderDialog implements View.OnClickListener, FileSelectDialog.IFileSelection {
    private final AddFolderCallback callback;
    private final WeakReference<Context> contextRef;
    private final boolean showImportFolder;
    private final ViewDialog dialog;
    private final View view;
    private View newDivider;
    private View importDivider;
    private View newFolderLayout;
    private View importFolderLayout;
    private EditText folderNameEt;

    ////////////////////////////////////////////////////////////////////////////////
    // Callback functions for folder selection
    private EditText folderPathEt;
    private boolean addFolder = true;

    public AddFolderDialog(final Context context, AddFolderCallback callback, boolean showImportFolder) {

        this.contextRef = new WeakReference<>(context);
        this.callback = callback;
        this.showImportFolder = showImportFolder;

        view = LayoutInflater.from(context).inflate(R.layout.dlg_add_folder, null);

        dialog = new ViewDialog(context);
        dialog.setTitle("New");
        dialog.setView(view);

        init();
    }

    public void show() {
        dialog.show();
    }

    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        folderPathEt.setText(filePath);
    }

    @Override
    public boolean isInterestedFile(String filename, String extraStr) {
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }

    private void init() {
        TextView newTv = (TextView) view.findViewById(R.id.tv_new_folder);
        TextView importTv = (TextView) view.findViewById(R.id.tv_import_folder);
        if (!showImportFolder) {
            importTv.setVisibility(View.GONE);
        }

        this.newDivider = view.findViewById(R.id.divider1);
        this.importDivider = view.findViewById(R.id.divider2);
        this.newFolderLayout = view.findViewById(R.id.layout_new);
        this.importFolderLayout = view.findViewById(R.id.layout_import);

        folderNameEt = (EditText) view.findViewById(R.id.et_folder_name);
        folderPathEt = (EditText) view.findViewById(R.id.et_folder_path);

        InputFilter filter = InputUtil.getFileNameFilter();
        folderNameEt.setFilters(new InputFilter[]{filter});

        newTv.setOnClickListener(this);
        importTv.setOnClickListener(this);
        view.findViewById(R.id.btn_browse).setOnClickListener(this);
        view.findViewById(R.id.btn_cancel).setOnClickListener(this);
        view.findViewById(R.id.btn_confirm).setOnClickListener(this);
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.tv_new_folder) {
            showNewFolderView();
        } else if (id == R.id.tv_import_folder) {
            showImportFolderView();
        } else if (id == R.id.btn_cancel) {
            dialog.dismiss();
        } else if (id == R.id.btn_confirm) {
            confirm();
        } else if (id == R.id.btn_browse) {
            browse();
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
                Toast.makeText(contextRef.get(),
                        R.string.empty_input_tip, Toast.LENGTH_LONG).show();
            } else {
                callback.addFolder(name);
                dialog.dismiss();
            }
        }
        // To import a folder
        else {
            String path = folderPathEt.getText().toString();
            path = path.trim();
            if ("".equals(path)) {
                Toast.makeText(contextRef.get(),
                        R.string.empty_input_tip, Toast.LENGTH_LONG).show();
            } else if (!new File(path).exists()) {
                String fmt = contextRef.get().getString(R.string.error_path_xxx_not_exist);
                String message = String.format(fmt, path);
                Toast.makeText(contextRef.get(), message, Toast.LENGTH_LONG).show();
            } else {
                callback.importFolder(path);
                dialog.dismiss();
            }
        }
    }

    private void showNewFolderView() {
        addFolder = true;
        newDivider.setVisibility(View.VISIBLE);
        importDivider.setVisibility(View.INVISIBLE);
        newFolderLayout.setVisibility(View.VISIBLE);
        importFolderLayout.setVisibility(View.INVISIBLE);
    }

    private void showImportFolderView() {
        addFolder = false;
        newDivider.setVisibility(View.INVISIBLE);
        importDivider.setVisibility(View.VISIBLE);
        newFolderLayout.setVisibility(View.INVISIBLE);
        importFolderLayout.setVisibility(View.VISIBLE);
    }

    public interface AddFolderCallback {
        void addFolder(String folderName);

        void importFolder(String folderPath);
    }
}

