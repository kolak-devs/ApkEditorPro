package com.mcal.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.MatchedFilenameAdapter;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.ResListAdapter;
import com.mcal.apkeditor.ResSelectionChangeListener;
import com.mcal.apkeditor.activities.ApkInfoActivity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// This activity is called from ApkInfoActivity::searchInResourceFiles
public class SearchFilenameDialog implements ResSelectionChangeListener,
        OnItemClickListener, OnItemLongClickListener {

    private final String mSearchFolder;
    private final List<String> mFilenameList;
    private final String mKeyword;
    private final boolean mCaseSensitive;
    // Record modified files
    WeakReference<ApkInfoActivity> activityRef;
    private TextView titleTv;
    private View selectionHeaderView;
    private TextView selectionTipTv;
    private ListView listView;
    private MatchedFilenameAdapter listAdapter;
    private LinearLayout searchingLayout;
    // Record matched files
    private ArrayList<String> matchedFiles = new ArrayList<>();
    private AlertDialog materialDialog;

    public SearchFilenameDialog(ApkInfoActivity activity, String searchFolder,
                                List<String> filenameList, String keyword, boolean caseSensitive) {
        activityRef = new WeakReference<>(activity);
        mSearchFolder = searchFolder;
        mFilenameList = filenameList;
        mKeyword = keyword;
        mCaseSensitive = caseSensitive;
        if (!mSearchFolder.endsWith("/")) {
            searchFolder += "/";
        }

        init(activity);
    }

    private void init(ApkInfoActivity activity) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_filename_searchret, null);

        titleTv = view.findViewById(R.id.title);
        selectionHeaderView = view.findViewById(R.id.res_header_selection);
        selectionTipTv = view.findViewById(R.id.selection_tip);
        listView = view.findViewById(R.id.file_list);
        searchingLayout = view.findViewById(R.id.searching_layout);

        ImageButton doneMenu = view.findViewById(R.id.menu_done);
        ImageButton selectMenu = view.findViewById(R.id.menu_select);
        doneMenu.setOnClickListener(v -> {
            listAdapter.selectNone();
            showNonSelectView();
        });
        selectMenu.setOnClickListener(v -> {
            if (listAdapter.isAllSelected()) {
                listAdapter.selectNone();
                showNonSelectView();
            } else {
                listAdapter.selectAll();
            }
        });

        listView.setVisibility(View.INVISIBLE);

        // Start searching task
        new AsyncFolderSearchTask(mSearchFolder, mFilenameList, mKeyword).execute();

        materialDialog = new MaterialAlertDialogBuilder(activity)
                .setView(view)
                .setPositiveButton(activity.getString(R.string.close), null)
                .setNegativeButton(activity.getString(R.string.delete), null)
                .create();
        materialDialog.setOnShowListener(dialogShowListener -> {
            materialDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> {
                deleteSelectedFiles();
                materialDialog.dismiss();
            });
            materialDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(View.GONE);
        });
        materialDialog.show();
    }

    private void showMatchedFiles() {
        // Set title
        String format = activityRef.get().getString(R.string.str_files_found);
        String text = String.format(format, matchedFiles.size(), mKeyword);
        titleTv.setText(text);

        listAdapter = new MatchedFilenameAdapter(activityRef.get(), this, mSearchFolder, matchedFiles);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        // Switch to list view
        listView.setVisibility(View.VISIBLE);
        searchingLayout.setVisibility(View.INVISIBLE);
    }

    private void deleteSelectedFiles() {
        List<Integer> selected = listAdapter.getSeletedItems();
        deleteFilesByIndex(selected);
    }

    private void deleteFilesByIndex(@NonNull List<Integer> indexes) {
        ResListAdapter resManager = activityRef.get().getResListAdapter();

        // Use ResListAdapter to delete it
        for (int index : indexes) {
            String filepath = this.matchedFiles.get(index);
            int pos = filepath.lastIndexOf('/');
            String dirPath = (pos != -1) ? filepath.substring(0, pos) : "";
            String fileName = filepath.substring(pos + 1);
            resManager.deleteFile(dirPath, fileName, false);
        }

        // Collect file list which not deleted
        ArrayList<String> fileList = new ArrayList<>();
        for (int i = 0; i < matchedFiles.size(); ++i) {
            if (!indexes.contains(i)) {
                fileList.add(matchedFiles.get(i));
            }
        }

        // Update matched files
        matchedFiles = fileList;
        listAdapter.resetFileList(matchedFiles, indexes);

        if (listAdapter.isNonSelected()) {
            showNonSelectView();
        }
    }

    private void showNonSelectView() {
        titleTv.setVisibility(View.VISIBLE);
        selectionHeaderView.setVisibility(View.INVISIBLE);

        // As file count may change, need to update the title
        String format = activityRef.get().getString(R.string.str_files_found);
        String text = String.format(format, matchedFiles.size(), mKeyword);
        titleTv.setText(text);
    }

    @Override
    public void selectionChanged(@NonNull Set<Integer> selected) {
        // No selection at all
        if (selected.isEmpty()) {
            showNonSelectView();
        } else {
            String text = String.format(activityRef.get().getString(R.string.num_items_selected), selected.size());
            selectionTipTv.setText(text);
            titleTv.setVisibility(View.INVISIBLE);
            selectionHeaderView.setVisibility(View.VISIBLE);
        }
        materialDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(selected.isEmpty() ? View.GONE : View.VISIBLE);
        materialDialog.show();
    }

    // Click on list item
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position >= matchedFiles.size()) {
            return;
        }

        String filepath = matchedFiles.get(position);
        int pos = filepath.lastIndexOf("/");
        if (pos != -1) {
            String directory = filepath.substring(0, pos);
            String fileName = filepath.substring(pos + 1);
            activityRef.get().openFile(directory, fileName, false);
        }
    }

    private void deleteItem(int position) {
        List<Integer> indexes = new ArrayList<>();
        indexes.add(position);
        this.deleteFilesByIndex(indexes);
    }

    private void extractItem(int position) {
        if (position < matchedFiles.size()) {
            String filepath = matchedFiles.get(position);
            activityRef.get().extractFileOrDir(filepath);
        }
    }

    private void replaceItem(int position) {
        if (position < matchedFiles.size()) {
            String filepath = matchedFiles.get(position);
            activityRef.get().replaceFile(filepath, () -> listAdapter.notifyDataSetChanged());
        }
    }

    // Long click on list item
    @Override
    public boolean onItemLongClick(@NonNull AdapterView<?> parent, View view,
                                   final int position, long id) {
        parent.setOnCreateContextMenuListener(
                (menu, v, menuInfo) -> {
                    // Delete
                    MenuItem item1 = menu.add(0, Menu.FIRST, 0,
                            R.string.delete);
                    item1.setOnMenuItemClickListener(
                            item -> {
                                deleteItem(position);
                                return true;
                            });
                    // Extract
                    MenuItem item2 = menu.add(0, Menu.FIRST + 1, 0,
                            R.string.extract);
                    item2.setOnMenuItemClickListener(
                            item -> {
                                extractItem(position);
                                return true;
                            });
                    // Replace the file
                    MenuItem item3 = menu.add(0, Menu.FIRST + 2, 0,
                            R.string.replace);
                    OnMenuItemClickListener listener = item -> {
                        replaceItem(position);
                        return true;
                    };
                    item3.setOnMenuItemClickListener(listener);
                });

        return false;
    }

    // Search all the files inside the folder
    @SuppressLint("StaticFieldLeak")
    private class AsyncFolderSearchTask
            extends AsyncTask<Object, Void, List<String>> {

        private final String mBaseFolder;
        private final List<String> mFilenameList;
        private final String mKeyword;
        private final String lowerCaseKeyword;

        @SuppressLint("DefaultLocale")
        public AsyncFolderSearchTask(String folderPath,
                                     List<String> filenameList, @NonNull String keyword) {
            mBaseFolder = folderPath;
            mFilenameList = filenameList;
            mKeyword = keyword;
            lowerCaseKeyword = keyword.toLowerCase();
        }

        // Check the file whether contains the keyword
        private boolean fileMatches(String filename) {
            boolean bContain;
            if (mCaseSensitive) {
                bContain = filename.contains(mKeyword);
            } else {
                bContain = filename.toLowerCase()
                        .contains(lowerCaseKeyword);
            }
            return bContain;
        }

        private void searchFolder(@NonNull File folderFile) {
            File[] files = folderFile.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        searchFolder(f);
                    } else {
                        if (fileMatches(f.getName())) {
                            SearchFilenameDialog.this.matchedFiles
                                    .add(f.getPath());
                        }
                    }
                }
            }
        }

        @Override
        protected List<String> doInBackground(Object... params) {
            File root = new File(mBaseFolder);
            for (String filename : mFilenameList) {
                File f = new File(root, filename);
                if (!f.exists()) { // Not exist
                    continue;
                }
                if (f.isDirectory()) {
                    searchFolder(f);
                } else { // Regular file
                    if (fileMatches(filename)) {
                        SearchFilenameDialog.this.matchedFiles.add(f.getPath());
                    }
                }
            }

            return matchedFiles;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            showMatchedFiles();
        }
    }
}