package com.mcal.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.MatchedFilenameAdapter;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.ResListAdapter;
import com.mcal.apkeditor.ResSelectionChangeListener;
import com.mcal.apkeditor.SomethingChangedListener;
import com.mcal.apkeditor.activities.ApkInfoActivity;
import com.mcal.apkeditor.view.ViewDialog;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// This activity is called from ApkInfoActivity::searchInResourceFiles
public class SearchFilenameDialog implements
        android.view.View.OnClickListener, ResSelectionChangeListener,
        OnItemClickListener, OnItemLongClickListener {

    private final String searchFolder;
    private final List<String> filenameList;
    private final String keyword;
    private final boolean caseSensitive;
    // Record modified files
    private final Set<String> modifiedFiles = new HashSet<>();
    WeakReference<ApkInfoActivity> activityRef;
    private TextView titleTv;
    private View selectionHeaderView;
    private TextView selectionTipTv;
    private ListView listView;
    private MatchedFilenameAdapter listAdapter;
    private LinearLayout searchingLayout;
    private Button closeBtn;
    private Button deleteBtn;
    private View doneMenu;
    private View selectMenu;
    // Record matched files
    private ArrayList<String> matchedFiles = new ArrayList<String>();
    private ViewDialog dialog;

    public SearchFilenameDialog(ApkInfoActivity activity, String searchFolder,
                                List<String> filenameList, String keyword, boolean caseSensitive) {
        this.activityRef = new WeakReference<>(activity);
        this.searchFolder = searchFolder;
        this.filenameList = filenameList;
        this.keyword = keyword;
        this.caseSensitive = caseSensitive;
        if (!this.searchFolder.endsWith("/")) {
            searchFolder += "/";
        }

        init(activity);
    }

    private void init(ApkInfoActivity activity) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dlg_filename_searchret, null);

        this.titleTv = view.findViewById(R.id.title);
        this.selectionHeaderView = view.findViewById(R.id.res_header_selection);
        this.selectionTipTv = view.findViewById(R.id.selection_tip);
        this.listView = view.findViewById(R.id.file_list);
        this.searchingLayout = view
                .findViewById(R.id.searching_layout);

        this.doneMenu = view.findViewById(R.id.menu_done);
        this.selectMenu = view.findViewById(R.id.menu_select);
        this.doneMenu.setOnClickListener(this);
        this.selectMenu.setOnClickListener(this);

        this.closeBtn = view.findViewById(R.id.btn_close);
        this.deleteBtn = view.findViewById(R.id.btn_delete);
        this.closeBtn.setOnClickListener(this);
        this.deleteBtn.setOnClickListener(this);

        listView.setVisibility(View.INVISIBLE);

        // Start searching task
        new AsyncFolderSearchTask(searchFolder, filenameList, keyword).execute();

        dialog = new ViewDialog(activity);
        dialog.setTitle("Search");
        dialog.setView(view);
        dialog.show();
    }

    private void showMatchedFiles() {
        // Set title
        String format = activityRef.get().getString(R.string.str_files_found);
        String text = String.format(format, matchedFiles.size(), keyword);
        titleTv.setText(text);

        this.listAdapter = new MatchedFilenameAdapter(activityRef.get(), this,
                searchFolder, matchedFiles);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        // Switch to list view
        listView.setVisibility(View.VISIBLE);
        searchingLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            dialog.dismiss();
        } else if (id == R.id.btn_delete) {
            deleteSelectedFiles();
        } else if (id == R.id.menu_done) {
            listAdapter.selectNone();
            this.showNonSelectView();
        } else if (id == R.id.menu_select) {
            if (listAdapter.isAllSelected()) {
                listAdapter.selectNone();
                this.showNonSelectView();
            } else {
                listAdapter.selectAll();
            }
        }
    }

    private void deleteSelectedFiles() {
        List<Integer> selected = listAdapter.getSeletedItems();
        deleteFilesByIndex(selected);
    }

    private void deleteFilesByIndex(List<Integer> indexes) {
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
        ArrayList<String> fileList = new ArrayList<String>();
        for (int i = 0; i < matchedFiles.size(); ++i) {
            if (!indexes.contains(i)) {
                fileList.add(matchedFiles.get(i));
            }
        }

        // Update matched files
        this.matchedFiles = fileList;
        listAdapter.resetFileList(this.matchedFiles, indexes);

        if (listAdapter.isNonSelected()) {
            showNonSelectView();
        }
    }

    private void showNonSelectView() {
        titleTv.setVisibility(View.VISIBLE);
        selectionHeaderView.setVisibility(View.INVISIBLE);
        deleteBtn.setVisibility(View.INVISIBLE);

        // As file count may change, need to update the title
        String format = activityRef.get().getString(R.string.str_files_found);
        String text = String.format(format, matchedFiles.size(), keyword);
        titleTv.setText(text);
    }

    @Override
    public void selectionChanged(Set<Integer> selected) {
        // No selection at all
        if (selected.isEmpty()) {
            showNonSelectView();
        } else {
            String text = String.format(
                    activityRef.get().getString(R.string.num_items_selected),
                    selected.size());
            selectionTipTv.setText(text);
            titleTv.setVisibility(View.INVISIBLE);
            selectionHeaderView.setVisibility(View.VISIBLE);
            deleteBtn.setVisibility(View.VISIBLE);
        }
    }

    // Click on list item
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
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
        List<Integer> indexes = new ArrayList<Integer>();
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
            activityRef.get().replaceFile(filepath,
                    new SomethingChangedListener() {
                        @Override
                        public void somethingChanged() {
                            listAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    // Long click on list item
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                   final int position, long id) {
        parent.setOnCreateContextMenuListener(
                new OnCreateContextMenuListener() {
                    public void onCreateContextMenu(ContextMenu menu, View v,
                                                    ContextMenuInfo menuInfo) {

                        // Delete
                        MenuItem item1 = menu.add(0, Menu.FIRST, 0,
                                R.string.delete);
                        item1.setOnMenuItemClickListener(
                                new OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(
                                            MenuItem item) {
                                        deleteItem(position);
                                        return true;
                                    }
                                });
                        // Extract
                        MenuItem item2 = menu.add(0, Menu.FIRST + 1, 0,
                                R.string.extract);
                        item2.setOnMenuItemClickListener(
                                new OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(
                                            MenuItem item) {
                                        extractItem(position);
                                        return true;
                                    }
                                });
                        // Replace the file
                        MenuItem item3 = menu.add(0, Menu.FIRST + 2, 0,
                                R.string.replace);
                        OnMenuItemClickListener listener = new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                replaceItem(position);
                                return true;
                            }
                        };
                        item3.setOnMenuItemClickListener(listener);
                    }
                });

        return false;
    }

    // Search all the files inside the folder
    @SuppressLint("StaticFieldLeak")
    private class AsyncFolderSearchTask
            extends AsyncTask<Object, Void, List<String>> {

        private final String baseFolder;
        private final List<String> filenameList;
        private final String keyword;
        private final String lowerCaseKeyword;

        @SuppressLint("DefaultLocale")
        public AsyncFolderSearchTask(String folderPath,
                                     List<String> filenameList, @NonNull String keyword) {
            this.baseFolder = folderPath;
            this.filenameList = filenameList;
            this.keyword = keyword;
            this.lowerCaseKeyword = keyword.toLowerCase();
        }

        // Check the file whether contains the keyword
        private boolean fileMatches(String filename) {
            boolean bContain = false;
            if (caseSensitive) {
                bContain = filename.contains(keyword);
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
            File root = new File(baseFolder);
            for (String filename : filenameList) {
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
