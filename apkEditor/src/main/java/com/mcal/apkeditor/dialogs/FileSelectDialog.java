package com.mcal.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.ResListAdapter;
import com.mcal.common.utilsOld.InputUtils;
import com.mcal.common.utilsOld.PathUtils;
import com.mcal.common.utilsOld.SDCard;
import com.mcal.folderlist.FileRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileSelectDialog implements OnItemClickListener, AdapterView.OnItemLongClickListener {
    private static final String LAST_DIR = "lastDirectory";

    private final ResListAdapter fileListAdapter;

    // private ApkInfoActivity activity;
    // In most cases, the extra string is the replaced file path
    private final String extraStr;

    // To select a folder or not
    private final boolean mSelectFolder;

    // tag to save last directory
    private final String mTag;

    private final TextView pathTv;

    private final CheckBox editCheckBox;

    private final IFileSelection mCallback;

    private final Context mContext;
    private final AlertDialog dialog;

    // extraString should be the replaced file name if used to replace a file
    public FileSelectDialog(Context ctx, IFileSelection callback,
                            String fileSuffix, String extraString, String strTitle) {
        this(ctx, callback, fileSuffix, extraString, strTitle, false, false, false, null);
    }

    public FileSelectDialog(Context ctx, IFileSelection callback,
                            String fileSuffix, String extraString, String strTitle,
                            boolean selectFolder, boolean showConfirmDlg, boolean showEditOption,
                            String tag) {
        this(ctx, callback,
                fileSuffix, extraString, strTitle,
                selectFolder, showConfirmDlg, showEditOption,
                tag, null);
    }

    // When selectFolder = true, means to select a folder
    // tag is used to differentiate remembered directory
    @SuppressLint("InflateParams")
    public FileSelectDialog(Context ctx, IFileSelection callback,
                            String fileSuffix, String extraString, String strTitle,
                            boolean selectFolder, boolean showConfirmDlg, boolean showEditOption,
                            String tag, String defaultDir) {
        mContext = ctx;
        mCallback = callback;
        extraStr = extraString;
        mSelectFolder = selectFolder;
        // Should show confirmation dialog or not
        mTag = tag;

        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_fileselect, null, false);

        // File List view
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String key = LAST_DIR;
        if (tag != null) {
            key = tag + "_" + LAST_DIR;
        }
        String lastDir = sp.getString(key, "");
        if (!new File(lastDir).exists()) {
            if (defaultDir == null) {
                lastDir = SDCard.getRootDirectory();
            } else {
                lastDir = defaultDir;
            }
        }

        // Title & sub title
        // Sub title for current path
        TextView titleTv = view.findViewById(R.id.tv_title);
        pathTv = view.findViewById(R.id.tv_subtitle);
        if (strTitle == null) {
            if (fileSuffix != null) {
                strTitle = ctx.getString(R.string.select_file_replace) + " (" + fileSuffix + ")";
            } else {
                strTitle = ctx.getString(R.string.select_file_replace);
            }
        }
        titleTv.setText(strTitle);
        pathTv.setText(lastDir);

        // File list view
        ListView fileList = view.findViewById(R.id.file_list);
        this.fileListAdapter = new ResListAdapter(ctx, null, lastDir, "/",
                (dir, filename) -> {
                    File f = new File(dir, filename);
                    return (f.isDirectory() || isInterestedFile(filename));
                });
        fileList.setAdapter(fileListAdapter);
        fileList.setOnItemClickListener(this);
        fileList.setOnItemLongClickListener(this);

        // Checkbox (Edit it before replace)
        this.editCheckBox = view.findViewById(R.id.cb_edit_before_replace);
        if (showEditOption) {
            editCheckBox.setText(
                    String.format(ctx.getString(R.string.edit_before_replace),
                            PathUtils.getNameFromPath(extraString)));
            editCheckBox.setChecked(getHistoryEditOption());
            editCheckBox.setVisibility(View.VISIBLE);
        } else {
            editCheckBox.setVisibility(View.GONE);
        }

        dialog = new MaterialAlertDialogBuilder(ctx).setView(view).create();
        if (selectFolder) {
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, ctx.getString(android.R.string.ok), (dialog, which) -> {
                final String curDir = fileListAdapter.getData(null);
                if (showConfirmDlg) {
                    new MaterialAlertDialogBuilder(ctx)
                            .setTitle(R.string.confirm_dir_replace)
                            .setMessage(callback.getConfirmMessage(curDir, extraStr))
                            .setPositiveButton(R.string.yes,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            callback.fileSelectedInDialog(
                                                    curDir, extraStr, isEditSelected());
                                            saveLastDirectory(curDir);
                                            close();
                                        }
                                    })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                } else {
                    callback.fileSelectedInDialog(curDir, extraStr, isEditSelected());
                    saveLastDirectory(curDir);
                    close();
                }
                dialog.dismiss();
            });
        }
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, ctx.getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public boolean onItemLongClick(@NonNull AdapterView<?> parent, View view, int position, long id) {
        parent.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            // New Folder
            MenuItem item1 = menu.add(0, Menu.FIRST, 0, R.string.new_folder);
            item1.setOnMenuItemClickListener(item -> {
                createFolder();
                return true;
            });
        });
        return false;
    }

    private void createFolder() {
        final String dirPath = fileListAdapter.getData(null);

        MaterialAlertDialogBuilder inputDlg = new MaterialAlertDialogBuilder(mContext);
        inputDlg.setTitle(R.string.new_folder);
        inputDlg.setMessage(R.string.pls_input_foldername);

        // Set an EditText view to get user input
        final EditText input = new EditText(mContext);
        InputFilter filter = InputUtils.getFileNameFilter();
        input.setFilters(new InputFilter[]{filter});
        inputDlg.setView(input);

        inputDlg.setPositiveButton(android.R.string.ok,
                (dialog, whichButton) -> {
                    String name = input.getText().toString();
                    name = name.trim();
                    if ("".equals(name)) {
                        Toast.makeText(mContext,
                                        R.string.empty_input_tip, Toast.LENGTH_LONG)
                                .show();
                    } else {
                        fileListAdapter.addFolder(dirPath, name);
                    }
                });

        inputDlg.setNegativeButton(android.R.string.cancel, null);
        inputDlg.show();
    }

    // To decide whether to show the file in the dialog
    private boolean isInterestedFile(String filename) {
        return mCallback.isInterestedFile(filename, extraStr);
    }

    protected void close() {
        dialog.dismiss();
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        List<FileRecord> fileList = new ArrayList<>();
        String oldDir = fileListAdapter.getData(fileList);
        FileRecord rec = fileList.get(position);
        if (rec == null) {
            return;
        }

        if (rec.isDir) {
            String targetPath;
            if (rec.fileName.equals("..")) {
                int pos = oldDir.lastIndexOf('/');
                targetPath = oldDir.substring(0, pos);
            } else {
                targetPath = oldDir + "/" + rec.fileName;
            }
            fileListAdapter.openDirectory(targetPath);

            String curPath = fileListAdapter.getData(null);
            pathTv.setText(curPath);

        } else if (!mSelectFolder && isInterestedFile(rec.fileName)) {
            String selectedPath = oldDir + "/" + rec.fileName;
            boolean editSelected = isEditSelected();
            mCallback.fileSelectedInDialog(selectedPath, extraStr, editSelected);
            saveLastDirectory(oldDir);
            saveEditOption(editSelected);
            close();
        }
    }

    // Save the directory as the default directory next time
    private void saveLastDirectory(String lastDir) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        Editor editor = sp.edit();
        String key = LAST_DIR;
        if (mTag != null) {
            key = mTag + "_" + LAST_DIR;
        }
        editor.putString(key, lastDir);
        editor.apply();
    }

    private boolean getHistoryEditOption() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        String key = "editBeforeReplace";
        if (mTag != null) {
            key = mTag + "_" + key;
        }
        return sp.getBoolean(key, false);
    }

    private void saveEditOption(boolean editChecked) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        Editor editor = sp.edit();
        String key = "editBeforeReplace";
        if (mTag != null) {
            key = mTag + "_" + key;
        }
        editor.putBoolean(key, editChecked);
        editor.apply();
    }

    // If the option "Edit the file before replacing" selected
    private boolean isEditSelected() {
        return editCheckBox.isChecked();
    }

    public interface IFileSelection {
        void fileSelectedInDialog(@Nullable String filePath, @Nullable String extraStr, boolean openFile);

        // Only show interested file
        boolean isInterestedFile(@Nullable String filename, @Nullable String extraStr);

        // For folder replacement, will call it to show the message
        @Nullable
        String getConfirmMessage(@Nullable String filePath, @Nullable String extraStr);
    }
}
