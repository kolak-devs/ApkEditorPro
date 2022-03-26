package com.mcal.apkeditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import com.mcal.apkeditor.ac.AutoCompleteAdapter;
import com.mcal.common.utils.ClipboardUtil;
import com.mcal.common.utils.IOUtils;
import com.mcal.common.utils.RandomUtil;
import com.mcal.common.utils.SDCard;
import com.mcal.common.utils.ZipUtil;
import com.mcal.folderlist.FileRecord;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ApkInfoExActivity extends ApkInfoActivity {

    // Click listener Stub
    private final MenuClickListener clickListener = new MenuClickListener();
    private LinearLayout menuLayout;
    private View menuItem_home;
    private View menuItem_done;
    private View menuItem_select;
    private View menuItem_addFile;
    private View menuItem_addDir;
    private View menuItem_searchOptions;
    private View menuItem_searchCaseS; // case sensitive or insensitive
    private View menuItem_save;
    private View menuItem_delete;
    private View menuItem_search;
    private View menuItem_replace;
    private View menuItem_details;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initResourceMenu();
    }

    // Search file name or file content
    public void reverseSearchOption() {
        searchTextContent = !searchTextContent;

        searchOptionImage.setImageResource(searchTextContent ? R.drawable.round_feed_24
                : R.drawable.round_feed_blue_24);
    }

    public void reverseSearchCaseSensitive() {
        searchResSensitive = !searchResSensitive;

        searchOptionCase.setImageResource(searchResSensitive ? R.drawable.round_text_format_blue_24
                : R.drawable.round_text_format_24);
    }

    private void initResourceMenu() {
        // Menu when no file selected
        this.menuItem_home = this.findViewById(R.id.menu_home);
        this.menuItem_done = this.findViewById(R.id.menu_done);
        this.menuItem_select = this.findViewById(R.id.menu_select);
        this.menuItem_addFile = this.findViewById(R.id.menu_addfile);
        this.menuItem_addDir = this.findViewById(R.id.menu_addfolder);
        this.menuItem_searchOptions = findViewById(R.id.menu_searchoptions);
        this.menuItem_searchCaseS = findViewById(R.id.menu_caseinsensitive);
        menuItem_home.setOnClickListener(clickListener);
        menuItem_done.setOnClickListener(clickListener);
        menuItem_select.setOnClickListener(clickListener);
        menuItem_addFile.setOnClickListener(clickListener);
        menuItem_addDir.setOnClickListener(clickListener);
        menuItem_searchOptions.setOnClickListener(clickListener);
        menuItem_searchCaseS.setOnClickListener(clickListener);

        // Menu when file is selected
        this.menuLayout = this.findViewById(R.id.res_menu_layout);

        this.menuItem_save = createMenuItem(R.drawable.round_save_24, R.string.extract);
        this.menuItem_replace = createMenuItem(R.drawable.round_content_copy_24, R.string.replace);
        this.menuItem_search = createMenuItem(R.drawable.round_search_24, R.string.search);
        this.menuItem_delete = createMenuItem(R.drawable.round_delete_24, R.string.delete);
        this.menuItem_details = createMenuItem(R.drawable.round_menu_24, R.string.detail);

        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1.0f);
        this.menuLayout.addView(menuItem_save, param);
        this.menuLayout.addView(createVerticalLine());
        this.menuLayout.addView(menuItem_replace, param);
        this.menuLayout.addView(createVerticalLine());
        this.menuLayout.addView(menuItem_search, param);
        this.menuLayout.addView(createVerticalLine());
        this.menuLayout.addView(menuItem_delete, param);
        this.menuLayout.addView(createVerticalLine());
        this.menuLayout.addView(menuItem_details, param);

        // enableMenuItem(menuItem_replace, false);
        // enableMenuItem(menuItem_details, false);
    }

    @NonNull
    private View createVerticalLine() {
        View line = new View(this);
        line.setLayoutParams(
                new LinearLayout.LayoutParams(1, LayoutParams.MATCH_PARENT));

        line.setBackgroundColor(0xffe3e3e3);
        return line;
    }

    // drawable2 is for dark theme
    @NonNull
    @SuppressLint("InflateParams")
    private View createMenuItem(int drawable, int title) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_res_menu, null);
        ImageView icon = view.findViewById(R.id.menu_icon);
        icon.setImageResource(drawable);
        TextView tv = view.findViewById(R.id.menu_title);
        tv.setText(title);
        view.setId(drawable); // borrow the drawable id
        view.setOnClickListener(this.clickListener);
        return view;
    }

    private void enableMenuItem(@NonNull View view, boolean enabled) {
        ImageView icon = view.findViewById(R.id.menu_icon);
        TextView tv = view.findViewById(R.id.menu_title);
        if (enabled) {
            icon.getDrawable().setAlpha(255);
            tv.setEnabled(true);
            tv.setTextColor(0xff333333);
        } else {
            icon.getDrawable().setAlpha(80);
            tv.setEnabled(false);
            tv.setTextColor(0xff808080);
        }
        view.setClickable(enabled);
        view.setEnabled(enabled);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Resource selection changed
    @Override
    public void selectionChanged(Set<Integer> selected) {
        super.selectionChanged(selected);
        if (selected.size() == 1) {
            this.enableMenuItem(this.menuItem_replace, true);
            this.enableMenuItem(this.menuItem_details, true);
        } else {
            this.enableMenuItem(this.menuItem_replace, false);
            this.enableMenuItem(this.menuItem_details, false);
        }
    }

    class MenuClickListener implements OnClickListener {
        @Override
        public void onClick(@NonNull View v) {
            int id = v.getId();
            // Got to home/root resource directory
            if (id == R.id.menu_home) {
                gotoRootDirectory();
            }
            // Exit the selection mode
            else if (id == R.id.menu_done) {
                ApkInfoExActivity.this.resListAdapter.checkAllItems(false);
            }
            // Select all or select none
            else if (id == R.id.menu_select) {
                selectAllOrNone();
            }
            // Add a file
            else if (id == R.id.menu_addfile) {
                ApkInfoExActivity.this.addFile(0);
            }
            // Add a folder
            else if (id == R.id.menu_addfolder) {
                ApkInfoExActivity.this.createFolder(0);
            }
            // Search option (text or filename)
            else if (id == R.id.menu_searchoptions) {
                ApkInfoExActivity.this.reverseSearchOption();
            } else if (id == R.id.menu_caseinsensitive) {
                ApkInfoExActivity.this.reverseSearchCaseSensitive();
            }

            // Save
            else if (id == R.drawable.round_save_24) {
                saveResourcesTo();
            }

            // Replace
            else if (id == R.drawable.round_content_copy_24) {
                replaceFileOrFolder();
            }

            // Search
            else if (id == R.drawable.round_search_24) {
                inputKeywordAndSearch();
            }

            // Delete
            else if (id == R.drawable.round_delete_24) {
                deleteSelectedResources();
            }
            // Details/information
            else if (id == R.drawable.round_menu_24) {
                showResourceInformation();
            }
        }

        private void inputKeywordAndSearch() {
            AlertDialog.Builder inputDlg = new AlertDialog.Builder(
                    ApkInfoExActivity.this);
            inputDlg.setTitle(R.string.search);
            inputDlg.setMessage(R.string.pls_input_keyword);

            // Set an EditText view to get user input
            AutoCompleteAdapter adapter = new AutoCompleteAdapter(
                    getApplicationContext(), "res_keywords");

            LinearLayout layout = new LinearLayout(ApkInfoExActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));

            final AppCompatAutoCompleteTextView input = new AppCompatAutoCompleteTextView(ApkInfoExActivity.this);
            input.setAdapter(adapter);
            layout.addView(input, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));

            final CheckBox caseInsstCb = new CheckBox(ApkInfoExActivity.this);
            caseInsstCb.setText(R.string.case_insensitive);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 8, 0, 0);
            caseInsstCb.setLayoutParams(params);
            layout.addView(caseInsstCb);

            final CheckBox filenameCb = new CheckBox(ApkInfoExActivity.this);
            filenameCb.setText(R.string.search_file_names);
            LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            params2.setMargins(0, 8, 0, 32);
            filenameCb.setLayoutParams(params2);
            layout.addView(filenameCb);

            inputDlg.setView(layout);

            inputDlg.setPositiveButton(android.R.string.ok,
                    (dialog, whichButton) -> {
                        String keyword = input.getText().toString();
                        keyword = keyword.trim();
                        if ("".equals(keyword)) {
                            Toast.makeText(ApkInfoExActivity.this,
                                    R.string.empty_input_tip,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            boolean bSearchName = filenameCb.isChecked();
                            boolean bCaseIsst = caseInsstCb.isChecked();
                            doSearchInSelectedItems(keyword, bSearchName,
                                    bCaseIsst);
                        }
                    });

            inputDlg.setNegativeButton(android.R.string.cancel,
                    (dialog, whichButton) -> {
                        // Canceled.
                    });

            inputDlg.show();
        }

        // Keyword already input, now search in selected items
        protected void doSearchInSelectedItems(String keyword,
                                               boolean bSearchName, boolean bCaseIsst) {
            Set<Integer> selected = resListAdapter.getCheckedItems();
            if (selected.isEmpty()) {
                return;
            }

            List<FileRecord> records = new ArrayList<FileRecord>();
            String baseFolder = resListAdapter.getData(records);
            ArrayList<String> filenameList = new ArrayList<String>();
            ArrayList<Integer> positions = new ArrayList<Integer>(
                    selected.size());
            positions.addAll(selected);
            for (int index : positions) {
                filenameList.add(records.get(index).fileName);
            }

            // Call real search
            ApkInfoExActivity.this.searchInResourceFiles(keyword, baseFolder,
                    filenameList, bSearchName, !bCaseIsst);
        }

        private void showResourceInformation() {
            Set<Integer> selected = resListAdapter.getCheckedItems();
            if (selected.isEmpty()) {
                return;
            }

            int position = selected.iterator().next();

            // Check the item is directory or not
            List<FileRecord> records = new ArrayList<>();
            String curDir = resListAdapter.getData(records);
            FileRecord record = records.get(position);

            AlertDialog infoDlg = createInfoDialog(curDir, record, position);
            infoDlg.show();
        }

        // The detail/more/information dialog
        @NonNull
        private AlertDialog createInfoDialog(final String curDir,
                                             @NonNull final FileRecord record, final int position) {
            // Get file name and path
            String fileName = record.fileName;
            String filepath = curDir + "/" + record.fileName;
            final String relativePath = filepath.substring(decodeRootPath.length() + 1);

            // Get entry name
            String entryName = null;
            if (filepath.startsWith(decodeRootPath + "/")) {
                String fileEntry = filepath.substring(decodeRootPath.length() + 1);
                if (fileEntry2ZipEntry != null) {
                    entryName = fileEntry2ZipEntry.get(fileEntry);
                }
                if (entryName == null) {
                    entryName = fileEntry;
                }
                // Check if the entry exist
                ZipFile zipFile = null;
                try {
                    zipFile = new ZipFile(apkPath);
                    if (zipFile.getEntry(entryName) == null) {
                        entryName = null;
                    }
                } catch (Exception e) {
                    entryName = null;
                    e.printStackTrace();
                } finally {
                    closeQuietly(zipFile);
                }
            }

            // Create dialog view
            View view = LayoutInflater.from(ApkInfoExActivity.this).inflate(R.layout.dlg_resfile_more, null);
            final EditText et = view.findViewById(R.id.filename);
            et.setText(fileName);
            EditText et2 = view.findViewById(R.id.filepath);
            et2.setText(relativePath);
            EditText et3 = view.findViewById(R.id.fileentry);
            et3.setText(entryName != null ? entryName : getString(R.string.not_available));

            // Extract the original entry (for DEBUG)
            final String _entry = entryName;
            Button extractBtn = view.findViewById(R.id.btn_extract);
            if (record.isDir) {
                extractBtn.setVisibility(View.GONE);
            } else {
                extractBtn.setOnClickListener(v -> {
                    try {
                        ZipUtil.unzipFileTo(apkPath, _entry, "/sdcard/axml");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // Setup rename button listener
            Button renameBtn = view.findViewById(R.id.btn_rename);
            if (!isFullDecoding && record.isDir) {
                renameBtn.setVisibility(View.GONE);
            }
            renameBtn.setOnClickListener(v -> {
                final String newName = et.getText().toString().trim();
                // Empty input
                if (newName.equals("")) {
                    Toast.makeText(ApkInfoExActivity.this,
                            R.string.empty_input_tip,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                // Not changed
                if (newName.equals(record.fileName)) {
                    Toast.makeText(ApkInfoExActivity.this,
                            R.string.no_change_detected,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                // If file extension is changed, show tip
                if (!record.isDir && isExtensionChanged(record.fileName, newName)) {
                    AlertDialog.Builder dlg = new AlertDialog.Builder(
                            ApkInfoExActivity.this);
                    dlg.setMessage(R.string.extension_changed_tip);
                    dlg.setPositiveButton(R.string.yes,
                            (dialog, which) -> doFileRename(curDir, record, _entry,
                                    newName, position));
                    dlg.setNegativeButton(R.string.no, null);
                    dlg.show();
                } else {
                    doFileRename(curDir, record, _entry, newName, position);
                }
            });

            AlertDialog.Builder infoDlg = new AlertDialog.Builder(ApkInfoExActivity.this);
            infoDlg.setTitle(R.string.detail);
            infoDlg.setView(view);
            infoDlg.setNeutralButton(R.string.copy_file_path,
                    (dialog, which) -> {
                        Context ctx = ApkInfoExActivity.this;
                        ClipboardUtil.copyToClipboard(ctx, relativePath);
                        String msg = ctx
                                .getString(R.string.copied_to_clipboard);
                        msg = String.format(msg, relativePath);
                        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                    });
            infoDlg.setPositiveButton(android.R.string.ok, null);

            return infoDlg.create();
        }

        protected boolean isExtensionChanged(@NonNull String fileName, String newName) {
            // Origin name has no extension
            int pos = fileName.lastIndexOf('.');
            if (pos == -1) {
                return false;
            }

            String extension = fileName.substring(pos);

            pos = newName.lastIndexOf('.');
            if (pos == -1) {
                return true;
            }

            String newExt = newName.substring(pos);

            return !extension.equals(newExt);
        }

        // To secure the rename, we first remove the original one, and then add
        // a new one, but before that, need to make a copy either from decoded
        // file, or from the original apk
        protected void doFileRename(String curDir, FileRecord record,
                                    String entryName, String newName, int position) {
            // For full decoding, rename is very simple
            if (isFullDecoding) {
                File oldFile = new File(curDir + "/" + record.fileName);
                File newFile = new File(curDir + "/" + newName);
                oldFile.renameTo(newFile);
                return;
            }

            String tmpFilePath = null;

            // Prepare the file content
            boolean useFileSource = true;
            if (record.isInZip) {
                useFileSource = false;
            } else if (ApkInfoExActivity.isCommonImage(record.fileName)) {
                useFileSource = false;
            }

            ZipFile zipFile = null;
            InputStream input = null;
            FileOutputStream out = null;
            try {
                if (useFileSource) {
                    input = new FileInputStream(curDir + "/" + record.fileName);
                } else {
                    zipFile = new ZipFile(apkPath);
                    ZipEntry entry = zipFile.getEntry(entryName);
                    input = zipFile.getInputStream(entry);
                }

                tmpFilePath = SDCard.makeWorkingDir(ApkInfoExActivity.this)
                        + RandomUtil.getRandomString(6);
                out = new FileOutputStream(tmpFilePath);
                IOUtils.copy(input, out);
            } catch (Exception e) {
                Toast.makeText(ApkInfoExActivity.this,
                        R.string.str_rename_failed, Toast.LENGTH_SHORT).show();
                return;
            } finally {
                closeQuietly(out);
                closeQuietly(input);
                closeQuietly(zipFile);
            }

            // Delete the old entry
            resListAdapter.deleteFile(curDir, record.fileName, record.isInZip);
            List<Integer> positions = new ArrayList<Integer>();
            positions.add(position);
            resListAdapter.listItemsDeleted(positions);

            // resListAdapter.addFile(targetPath, filePath);
            rename_addNewFile(curDir, curDir + "/" + newName, tmpFilePath);
        }

        private void rename_addNewFile(String dirPath, String targetPath,
                                       String filePath) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(filePath);
                if (fis != null) {
                    FileRecord rec = resListAdapter.addFile(targetPath, fis);
                    if (rec != null) {
                        resListAdapter.listItemAdded(dirPath, rec);
                        Toast.makeText(ApkInfoExActivity.this,
                                R.string.file_renamed, Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(ApkInfoExActivity.this,
                        R.string.str_rename_failed, Toast.LENGTH_SHORT).show();
            } finally {
                closeQuietly(fis);
            }
        }

        private void closeQuietly(Closeable c) {
            if (c != null) {
                try {
                    c.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void closeQuietly(ZipFile file) {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void gotoRootDirectory() {
            resListAdapter.openDirectory(decodeRootPath);
            navigationMgr.gotoDirectory(decodeRootPath);
        }

        private void deleteSelectedResources() {
            resListAdapter.dumpChangedFiles(); // Debug

            Set<Integer> checked = ApkInfoExActivity.this.resListAdapter
                    .getCheckedItems();
            if (checked.isEmpty()) {
                return;
            }

            List<Integer> selected = new ArrayList<>();
            selected.addAll(checked);
            Collections.sort(selected);

            resListAdapter.deleteFile(selected);
        }

        private void saveResourcesTo() {
            Set<Integer> checked = ApkInfoExActivity.this.resListAdapter
                    .getCheckedItems();
            if (checked.isEmpty()) {
                return;
            }

            List<Integer> selected = new ArrayList<>();
            selected.addAll(checked);
            Collections.sort(selected);

            ApkInfoExActivity.this.extractFileOrDir(selected);
        }

        private void selectAllOrNone() {
            Set<Integer> checked = ApkInfoExActivity.this.resListAdapter
                    .getCheckedItems();
            int count = resListAdapter.getCount();
            List<FileRecord> records = new ArrayList<>(count);
            resListAdapter.getData(records);
            if ("..".equals(records.get(0).fileName)) { // Do not count the
                // parent folder
                count -= 1;
            }

            // some are not selected
            resListAdapter.checkAllItems(checked.size() != count);
        }

        private void replaceFileOrFolder() {
            Set<Integer> selected = resListAdapter.getCheckedItems();
            if (selected.isEmpty()) {
                return;
            }

            int position = selected.iterator().next();

            // Check the item is directory or not
            List<FileRecord> records = new ArrayList<>();
            resListAdapter.getData(records);
            boolean isDir = records.get(position).isDir;

            if (isDir) {
                replaceFolder(position);
            } else {
                replaceFile(position);
            }
        }
    }
}