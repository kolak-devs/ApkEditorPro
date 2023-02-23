package com.mcal.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.activities.ApkInfoActivity;
import com.mcal.apkeditor.adapters.MatchedTextListAdapter;
import com.mcal.apkeditor.adapters.ResListAdapter;
import com.mcal.common.view.AutoCompleteAdapter;
import com.mcal.common.view.AutoCompleteTextView;
import com.mcal.common.view.ProgressDialog;
import com.mcal.editor.TextEditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SearchTextDialog implements OnGroupClickListener, OnChildClickListener, AdapterView.OnItemLongClickListener {
    private final WeakReference<ApkInfoActivity> mActivityRef;
    private final String mSearchFolder;
    private final List<String> mFilenameList;
    private final String mKeyword;
    private final boolean mCaseSensitive;
    // Record matched files
    private final ArrayList<String> matchedFiles = new ArrayList<>();
    private TextView titleTv;
    private AutoCompleteTextView etReplaceAll;
    private ExpandableListView listView;
    private MatchedTextListAdapter listAdapter;
    private LinearLayout searchingLayout;
    // Replace string
    private AutoCompleteAdapter adapter;

    public SearchTextDialog(ApkInfoActivity activity, String searchFolder,
                            List<String> filenameList,
                            String keyword, boolean caseSensitive) {
        mActivityRef = new WeakReference<>(activity);
        mSearchFolder = searchFolder;
        mFilenameList = filenameList;
        mKeyword = keyword;
        mCaseSensitive = caseSensitive;
        if (!mSearchFolder.endsWith("/")) {
            searchFolder += "/";
        }
        init(activity);
    }

    private void init(Activity activity) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_txt_searchresult, null);

        titleTv = view.findViewById(R.id.title);
        etReplaceAll = view.findViewById(R.id.et_replaceall);
        listView = view.findViewById(R.id.lv_matchedfiles);
        searchingLayout = view.findViewById(R.id.searching_layout);
        listView.setVisibility(View.INVISIBLE);

        // Start searching task
        new AsyncFolderSearchTask(mSearchFolder, mFilenameList, mKeyword, mCaseSensitive).execute();

        // Replace all
        view.findViewById(R.id.btn_replaceall).setOnClickListener(v -> showConfirmDialog());
        adapter = new AutoCompleteAdapter(activity.getApplicationContext(), "search_replace_with");
        AutoCompleteTextView etReplaceAll = view.findViewById(R.id.et_replaceall);
        etReplaceAll.setAdapter(adapter);

        AlertDialog materialDialog = new MaterialAlertDialogBuilder(activity)
                .setView(view)
                .create();
        materialDialog.show();
    }

    @Override
    public boolean onGroupClick(@NonNull ExpandableListView parent, View v, int groupPosition, long id) {
        boolean expanded = parent.isGroupExpanded(groupPosition);
        if (!expanded) {
            // Search it only when never searched before
            if (!listAdapter.groupChildExist(groupPosition)) {
                String filePath = (String) listAdapter.getGroup(groupPosition);
                String keyword = listAdapter.getKeyword();
                new AsyncFileSearchTask(filePath, keyword, groupPosition).execute();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
                                int groupPos, int childPos, long id) {
        ArrayList<String> filePathList = listAdapter.getFileList();

        MatchedLineItem item = (MatchedLineItem) listAdapter.getChild(groupPos, childPos);
        if (item == null) {
            return false;
        }

        ApkInfoActivity activity = mActivityRef.get();
        Intent intent;
        // If too many files, only edit current file
        if (filePathList.size() > 100) {
            String filePath = filePathList.get(groupPos);
            intent = TextEditor.getSoraEditor(activity, filePath, activity.getApkPath(), item.lineIndex, null);
        } else {
            ArrayList<Integer> startLineList = new ArrayList<>(filePathList.size());
            for (int i = 0; i < groupPos; ++i) {
                startLineList.add(-1);
            }
            startLineList.add(item.lineIndex);
            for (int i = groupPos + 1; i < filePathList.size(); ++i) {
                startLineList.add(-1);
            }
            intent = TextEditor.getSoraEditor(activity, filePathList, groupPos, activity.getApkPath(), startLineList, mKeyword);
        }
        activity.startActivityForResult(intent, 0);
        return false;
    }

    private void showMatchedFiles() {
        // Set title
        String format = mActivityRef.get().getString(R.string.str_files_found);
        String text = String.format(format, matchedFiles.size(), mKeyword);
        titleTv.setText(text);

        listAdapter = new MatchedTextListAdapter(mActivityRef, listView, mSearchFolder, matchedFiles, mKeyword);
        listView.setAdapter(listAdapter);
        listView.setOnGroupClickListener(this);
        listView.setOnChildClickListener(this);
        listView.setOnItemLongClickListener(this);

        // Switch to list view
        listView.setVisibility(View.VISIBLE);
        searchingLayout.setVisibility(View.INVISIBLE);
    }

    private void showConfirmDialog() {
        final Activity activity = mActivityRef.get();
        final String strReplace = etReplaceAll.getText().toString();
        AlertDialog confirmDlg = new MaterialAlertDialogBuilder(activity).create();
        String msg = String.format(activity.getString(R.string.sure_to_replace_all), mKeyword, strReplace);
        confirmDlg.setMessage(msg);
        confirmDlg.setButton(DialogInterface.BUTTON_POSITIVE, activity.getString(android.R.string.ok), (dialog, which) -> {
            if (!"".equals(strReplace.trim())) {
                adapter.addInputHistory(strReplace);
            }
            doReplaceAll(strReplace);
            dialog.dismiss();
        });
        confirmDlg.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss());
        confirmDlg.show();
    }

    protected void doReplaceAll(final String strReplace) {
        final Activity activity = mActivityRef.get();
        new ProgressDialog(activity, "", "Working…", false,
                new ProgressDialog.ProcessingInterface() {
                    int failedNum = 0;
                    String failMessage = "";

                    @Override
                    public void process() {
                        for (String f : matchedFiles) {
                            try {
                                listAdapter.replaceWith(f, strReplace);
                                addModification(f);
                            } catch (Exception e) {
                                failMessage = String.format("%s\n%s", failMessage, String.format(
                                        activity.getString(R.string.failed_to_modify), f));
                                failedNum += 1;
                            }
                        }
                    }

                    @Override
                    public void afterProcess() {
                        for (int i = 0; i < listAdapter.getGroupCount(); ++i) {
                            listView.collapseGroup(i);
                            listAdapter.removeSearchResult(i);
                        }

                        String msg = mActivityRef.get().getString(R.string.str_num_modified_file);
                        msg = String.format(msg, matchedFiles.size() - failedNum);

                        if (failedNum > 0) {
                            msg += failMessage;
                            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, -1).show();
    }

    // Mark the file as modified
    public void addModification(String filePath) {
        mActivityRef.get().dealWithModifiedFile(filePath, null);
    }

    // Long click on list item
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                   int position, long id) {
        int itemType = ExpandableListView.getPackedPositionType(id);
        if (itemType != ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            return true;
        }

        final int groupIdx = ExpandableListView.getPackedPositionGroup(id);
        parent.setOnCreateContextMenuListener(
                (menu, v, menuInfo) -> {
                    // Delete
                    MenuItem item1 = menu.add(0, Menu.FIRST, 0, R.string.delete);
                    item1.setOnMenuItemClickListener(
                            item -> {
                                deleteItem(groupIdx);
                                return true;
                            });
                    // Extract
                    MenuItem item2 = menu.add(0, Menu.FIRST + 1, 0, R.string.extract);
                    item2.setOnMenuItemClickListener(
                            item -> {
                                extractItem(groupIdx);
                                return true;
                            });
                    // Replace the file
                    MenuItem item3 = menu.add(0, Menu.FIRST + 2, 0, R.string.replace);
                    MenuItem.OnMenuItemClickListener listener = item -> {
                        replaceItem(groupIdx);
                        return true;
                    };
                    item3.setOnMenuItemClickListener(listener);
                });
        return false;
    }

    private void deleteItem(int position) {
        ResListAdapter resManager = mActivityRef.get().getResListAdapter();

        // Use ResListAdapter to delete it
        String filepath = matchedFiles.get(position);
        int pos = filepath.lastIndexOf('/');
        String dirPath = (pos != -1) ? filepath.substring(0, pos) : "";
        String fileName = filepath.substring(pos + 1);
        resManager.deleteFile(dirPath, fileName, false);

        // Update UI
        listAdapter.removeItem(position);
    }

    private void extractItem(int position) {
        if (position < matchedFiles.size()) {
            String filepath = matchedFiles.get(position);
            mActivityRef.get().extractFileOrDir(filepath);
        }
    }

    private void replaceItem(final int position) {
        if (position < matchedFiles.size()) {
            String filepath = matchedFiles.get(position);
            mActivityRef.get().replaceFile(filepath,
                    () -> {
                        listView.collapseGroup(position);
                        listAdapter.removeSearchResult(position);
                    });
        }
    }

    // Search all the files inside the folder
    @SuppressLint("StaticFieldLeak")
    private class AsyncFolderSearchTask extends AsyncTask<Object, Void, List<String>> {

        private final String baseFolder;
        private final List<String> mFilenameList;
        private final String mKeyword;
        private final String lcKeyword; // lower case
        private final boolean mCaseSensitive;

        @SuppressLint("DefaultLocale")
        public AsyncFolderSearchTask(String folderPath,
                                     List<String> filenameList, @NonNull String keyword,
                                     boolean caseSensitive) {
            baseFolder = folderPath;
            mFilenameList = filenameList;
            mKeyword = keyword;
            lcKeyword = keyword.toLowerCase();
            mCaseSensitive = caseSensitive;
        }

        // Check the file whether contains the keyword
        @SuppressLint("DefaultLocale")
        private boolean fileContainsKeyword(File file) {
            boolean ret = false;

            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

                // Search the keyword line by line
                String line = br.readLine();
                if (mCaseSensitive) {
                    while (line != null) {
                        if (line.contains(mKeyword)) {
                            ret = true;
                            break;
                        }
                        line = br.readLine();
                    }
                } else {
                    while (line != null) {
                        if (line.toLowerCase().contains(lcKeyword)) {
                            ret = true;
                            break;
                        }
                        line = br.readLine();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return ret;
        }

        private void searchFolder(@NonNull File folderFile) {
            File[] files = folderFile.listFiles();
            if (files != null)
                for (File f : files) {
                    if (f.isDirectory()) {
                        searchFolder(f);
                    } else if (isTxtFile(f)) {
                        if (fileContainsKeyword(f)) {
                            matchedFiles.add(f.getPath());
                        }
                    }
                }
        }

        private boolean isTxtFile(@NonNull File f) {
            String name = f.getName();
            return name.endsWith(".xml") ||
                    name.endsWith(".smali") ||
                    name.endsWith(".java") ||
                    name.endsWith(".json") ||
                    name.endsWith(".kt") ||
                    name.endsWith(".txt");
        }

        @Override
        protected List<String> doInBackground(Object... params) {
            File root = new File(baseFolder);
            for (String filename : mFilenameList) {
                File f = new File(root, filename);
                if (!f.exists()) { // Not exist
                    continue;
                }
                if (f.isDirectory()) {
                    searchFolder(f);
                } else if (isTxtFile(f)) { // Regular text file
                    if (fileContainsKeyword(f)) {
                        matchedFiles.add(f.getPath());
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

    // Search the keyword asynchronously in one file
    @SuppressLint("StaticFieldLeak")
    private class AsyncFileSearchTask extends AsyncTask<Object, Void, TxtSearchResult> {

        private final String mFilePath;
        private final String mKeyword;
        private final int mGroupPosition;

        public AsyncFileSearchTask(String filePath, String keyword, int groupPosition) {
            mFilePath = filePath;
            mKeyword = keyword;
            mGroupPosition = groupPosition;
        }

        @NonNull
        @SuppressLint("DefaultLocale")
        @Override
        protected TxtSearchResult doInBackground(Object... params) {
            TxtSearchResult result = new TxtSearchResult();
            result.filePath = mFilePath;
            result.keyword = mKeyword;
            String lcKeyword = mKeyword.toLowerCase();

            BufferedReader br = null;
            try {
                List<MatchedLineItem> matchedItems = new ArrayList<>();
                br = new BufferedReader(new InputStreamReader(new FileInputStream(mFilePath)));

                // Search the keyword line by line
                int lineIndex = 1;
                String line = br.readLine();
                while (line != null) {
                    int position;
                    if (mCaseSensitive) {
                        position = line.indexOf(mKeyword);
                    } else {
                        position = line.toLowerCase().indexOf(lcKeyword);
                    }
                    if (position != -1) {
                        matchedItems.add(new MatchedLineItem(lineIndex, position, line));
                    }
                    lineIndex += 1;
                    line = br.readLine();
                }

                result.matchList = matchedItems;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(@NonNull TxtSearchResult result) {
            // unfold the list
            if (result.matchList != null) {
                listAdapter.addSearchResult(result.filePath, result.matchList);
            }
            listView.expandGroup(mGroupPosition);
        }
    }
}