package com.mcal.folderlist;

import static com.mcal.common.utils.FileHelperKt.deleteAll;

import android.content.Context;
import android.text.InputFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.R;
import com.mcal.common.utils.InputHelper;
import com.mcal.folderlist.util.OpenFiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FolderListWrapper implements OnItemClickListener, OnItemLongClickListener {
    private final Context mContext;
    private final ListView mListView;
    private final String mRootPath;
    private final String mCurPath;

    private FolderListAdapter adapter;
    private final IListEventListener mListener;

    public FolderListWrapper(Context context, ListView listView, String curPath, String rootPath, IListEventListener listener, IListItemProducer producer) {
        mContext = context;
        mListView = listView;
        mCurPath = curPath;
        mRootPath = rootPath;
        mListener = listener;

        init(producer);
    }

    public FolderListAdapter getAdapter() {
        return adapter;
    }

    private void init(IListItemProducer producer) {
        adapter = new FolderListAdapter(mContext, mRootPath, mCurPath, producer);
        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                            long arg3) {
        final List<FileRecord> fileList = new ArrayList<>();
        final String oldDir = adapter.getData(fileList);
        final FileRecord rec = fileList.get(position);
        if (rec == null) {
            return;
        }

        if (rec.isDir) {
            final String targetPath;
            if (rec.fileName.equals("..")) {
                int pos = oldDir.lastIndexOf('/');
                targetPath = oldDir.substring(0, pos);
            } else {
                targetPath = oldDir + "/" + rec.fileName;
            }
            adapter.openDirectory(targetPath);
        } else {
            final String filePath = oldDir + "/" + rec.fileName;
            // When listener not deal with the opening, we will do it
            if (!mListener.fileClicked(arg1, filePath)) {
                OpenFiles.openFile(mContext, filePath);
            }
        }

        final String newDir = adapter.getData(null);
        if (!newDir.equals(oldDir)) {
            mListener.dirChanged(newDir);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        // The first item is always the parent folder
        if (position == 0) {
            return true;
        }
        parent.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            // Open As
            final MenuItem openAs = menu.add(0, Menu.FIRST, 0, "Open as...");
            openAs.setOnMenuItemClickListener(item -> {
                final List<FileRecord> fileList = new ArrayList<>();
                final String oldDir = adapter.getData(fileList);
                final FileRecord rec = fileList.get(position);
                if (rec != null) {
                    final String filePath = oldDir + "/" + rec.fileName;
                    OpenFiles.openFile(mContext, filePath);
                }
                return true;
            });

            // Delete
            final MenuItem item1 = menu.add(0, Menu.FIRST, 1, R.string.delete);
            item1.setOnMenuItemClickListener(item -> {
                deleteFile(position);
                return true;
            });

            // Rename
            final MenuItem item2 = menu.add(0, Menu.FIRST + 2, 0, R.string.rename);
            item2.setOnMenuItemClickListener(item -> {
                showRenameDlg(position);
                return true;
            });

            // New File
            final MenuItem item3 = menu.add(0, Menu.FIRST + 3, 0, R.string.new_file);
            item3.setOnMenuItemClickListener(item -> {
                createFile();
                return true;
            });

            if (mListener != null) {
                mListener.itemLongClicked(menu, v, menuInfo);
            }
        });
        return false;
    }

    private void showRenameDlg(int position) {
        final MaterialAlertDialogBuilder renameDlg = new MaterialAlertDialogBuilder(mContext);

        renameDlg.setTitle(R.string.rename);
        renameDlg.setMessage(R.string.pls_input_filename);

        // Set an EditText view to get user input
        final EditText input = new EditText(mContext);
        final List<FileRecord> records = new ArrayList<>();
        final String dirPath = adapter.getData(records);
        final FileRecord fr = records.get(position);
        if (fr == null) {
            return;
        }
        final String fileName = fr.fileName;
        input.setText(fileName);
        renameDlg.setView(input);

        renameDlg.setPositiveButton(android.R.string.ok,
                (dialog, whichButton) -> {
                    String newName = input.getText().toString();
                    boolean ret = doRename(dirPath, fileName, newName);
                    if (ret) {
                        adapter.fileRenamed(dirPath, fileName, newName);
                        mListener.fileRenamed(dirPath, fileName, newName);
                    }
                });

        renameDlg.setNegativeButton(android.R.string.cancel,
                (dialog, whichButton) -> {
                    // Canceled.
                });

        renameDlg.show();

    }

    protected boolean doRename(String dirPath, String fileName, String newName) {
        boolean ret = false;
        final File newFile = new File(dirPath + "/" + newName);
        if (newFile.exists()) {
            final String tip = mContext.getResources().getString(
                    R.string.file_already_exist);
            final String msg = String.format(tip, newName);
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        } else {
            ret = new File(dirPath + "/" + fileName).renameTo(newFile);
            final String strRename = mContext.getResources().getString(R.string.rename);
            final String strResult = mContext.getResources().getString(
                    ret ? R.string.succeed : R.string.failed);

            Toast.makeText(mContext, strRename + " " + strResult, Toast.LENGTH_SHORT)
                    .show();
        }
        return ret;
    }

    private void createFile() {
        final String dirPath = adapter.getData(null);

        final MaterialAlertDialogBuilder inputDlg = new MaterialAlertDialogBuilder(mContext);
        inputDlg.setTitle(R.string.new_file);
        inputDlg.setMessage(R.string.pls_input_filename);

        // Set an EditText view to get user input
        final EditText input = new EditText(mContext);
        final InputFilter filter = InputHelper.getFileNameFilter();
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
                        boolean succeed = false;
                        String errMessage = null;

                        // Try to create a new file in current directory
                        final File dir = new File(dirPath);
                        final File newFile = new File(dir, name);
                        try {
                            succeed = newFile.createNewFile();
                            if (succeed) {
                                // Update list view
                                adapter.openDirectory(dirPath);
                            } else {
                                errMessage = mContext.getString(R.string.failed_create_file);
                            }
                        } catch (IOException e) {
                            final String fmt = mContext.getString(R.string.general_error);
                            errMessage = String.format(fmt, e.getMessage());
                        }
                        if (!succeed) {
                            Toast.makeText(mContext, errMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });

        inputDlg.setNegativeButton(android.R.string.cancel,
                (dialog, whichButton) -> {
                    // Canceled.
                });

        inputDlg.show();
    }

    private void deleteFile(int position) {
        final List<FileRecord> records = new ArrayList<>();
        final String dirPath = adapter.getData(records);
        final FileRecord fr = records.get(position);
        if (fr == null) {
            return;
        }

        final String fileName = fr.fileName;
        final String path = dirPath + "/" + fr.fileName;
        final boolean ret = deleteFile(path);
        if (ret) {
            adapter.fileDeleted(dirPath, fileName);
            mListener.fileDeleted(dirPath, fileName);
        }
    }

    private boolean deleteFile(String path) {
        boolean ret = false;
        final File file = new File(path);

        if (file.exists()) {
            if (file.isFile()) {
                ret = file.delete();
            } else {
                try {
                    deleteAll(file);
                    ret = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return ret;
    }

    public void openDirectory(String dir) {
        final String oldDir = adapter.getData(null);

        // It may fail, so we need to check after the call
        adapter.openDirectory(dir);

        final String newDir = adapter.getData(null);

        if (!newDir.equals(oldDir)) {
            mListener.dirChanged(newDir);
        }
    }
}
