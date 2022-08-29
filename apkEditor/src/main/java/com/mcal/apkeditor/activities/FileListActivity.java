package com.mcal.apkeditor.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.util.LruCache;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.BuildConfig;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.dialogs.EditModeView;
import com.mcal.apkeditor.se.SimpleEditActivity;
import com.mcal.common.activities.CustomizedLangActivity;
import com.mcal.common.data.Preferences;
import com.mcal.common.utils.ScopedStorage;
import com.mcal.common.utilsOld.ActivityUtils;
import com.mcal.common.utils.ApkInfoParser;
import com.mcal.folderlist.FileRecord;
import com.mcal.folderlist.FolderListWrapper;
import com.mcal.folderlist.IListEventListener;
import com.mcal.folderlist.IListItemProducer;
import com.mcal.neweditor.TextEditor;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import ru.svolf.melissa.swipeback.SwipeBackActivity;

public class FileListActivity extends SwipeBackActivity implements IListEventListener,
        IListItemProducer, EditModeView.IEditModeSelected, OnClickListener {

    // Image cache
    private final LruCache<String, ApkInfoParser.AppInfo> apkIconCache = new LruCache<>(64);
    private MenuItem externalStorage;
    private String pathTV;
    private FolderListWrapper foderWrapper;

    @SuppressLint("HandlerLeak")
    private final android.os.Handler handler = new android.os.Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == 0) {
                foderWrapper.getAdapter().notifyDataSetChanged();
            }
        }
    };

    private ApkParseThread parseThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listfile);
        initFullScreen();

        // Parse Thread
        if (parseThread == null) {
            parseThread = new ApkParseThread();
            parseThread.start();
        }

        initWithPermChecking();
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
        parseThread.stopParse();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            init();
        }
    }

    private void initWithPermChecking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            init();
        }

    }

    private void init() {
        final String rootPath = "/";
        final String curDir = Preferences.getLastDirectory();

        pathTV = curDir;
        setupToolbar(R.id.toolbar, pathTV, true);

        final ListView listView = findViewById(R.id.file_list);
        foderWrapper = new FolderListWrapper(this, listView, curDir, rootPath, this, this);

        // Search apk files
        final AppCompatImageButton searchBtn = this.findViewById(R.id.search_button);
        searchBtn.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_filelist, menu);
        externalStorage = menu.findItem(R.id.external_storage);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.external_storage) {
            openExtSdCard();
        } else if (id == R.id.internal_storage) {
            openSdCard();
        } else if (id == R.id.app_files) {
            openAppFiles();
        } else if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        externalStorage.setVisible(isExistExtSdCard());
        return super.onPrepareOptionsMenu(menu);
    }

    private boolean isExistExtSdCard() {
        final String path = ScopedStorage.getExternalStoragePath();
        if (path != null && !path.equals("")) {
            return foderWrapper != null;
        }
        return false;
    }

    private boolean openExtSdCard() {
        final String path = ScopedStorage.getExternalStoragePath();
        if (path != null && !path.equals("")) {
            if (foderWrapper != null) {
                foderWrapper.openDirectory(path);
                return true;
            } else {
                return false;
            }
        } else {
            Toast.makeText(this, R.string.cannot_find_ext_sdcard,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean openAppFiles() {
        final File f = this.getFilesDir();
        if (foderWrapper != null) {
            foderWrapper.openDirectory(f.getPath());
            return true;
        } else {
            return false;
        }
    }

    private void openSdCard() {
        final String path = ScopedStorage.getStorageDirectory().getPath();
        if (!path.equals("")) {
            if (foderWrapper != null) {
                foderWrapper.openDirectory(path);
            }
        }
    }

    @Override
    public void dirChanged(String newDir) {
        if (pathTV != null) {
            setupToolbar(R.id.toolbar, newDir, true);
        }
    }

    @Override
    public void fileRenamed(String dirPath, String oldName, String newName) {
    }

    @Override
    public void fileDeleted(String dirPath, String fileName) {
    }

    @Override
    public void fileAdded(String fileName) {

    }

    @Override
    public void itemLongClicked(ContextMenu menu, View v,
                                ContextMenuInfo menuInfo) {
    }

    @Override
    public boolean fileClicked(View view, @NonNull String filePath) {
        if (filePath.endsWith(".apk")) {
            // Save the directory
            final String directory = filePath.substring(0, filePath.lastIndexOf('/'));
            Preferences.setLastDirectory(directory);

            if (BuildConfig.PARSER_ONLY) {
                UserAppActivity.startFullEditActivity(this, filePath);
            } else if (BuildConfig.LIMIT_NEW_VERSION && MainActivity.Companion.upgradedFromOldVersion(this)) {
                startFullEditActivity(filePath);
            } else {
                new EditModeView(this, this, filePath, null).showFileEditDialog();
            }
            return true;
        } else if (filePath.endsWith(".so")) {
            // Save the directory
            final String directory = filePath.substring(0, filePath.lastIndexOf('/'));
            Preferences.setLastDirectory(directory);

            final MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
            dialog.setTitle(R.string.app_translator);
            dialog.setMessage(R.string.open_binary_translator);
            dialog.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                final Intent intent = new Intent(FileListActivity.this, com.mcal.elfeditor.MainActivity.class);
                intent.putExtra("file_path", filePath);

                startActivity(intent);
                dialogInterface.dismiss();
            });
            dialog.setNegativeButton(android.R.string.cancel, null);
            dialog.show();
            return true;
        } else if (filePath.endsWith(".java") || filePath.endsWith(".kt") || filePath.endsWith(".xml") ||
                filePath.endsWith(".smali") || filePath.endsWith(".json") || filePath.endsWith(".cpp") ||
                filePath.endsWith(".c") || filePath.endsWith(".h") || filePath.endsWith(".hpp") || filePath.endsWith(".txt")) {
            final Intent intent = TextEditor.getSoraEditor(this, filePath, null, 0, null);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private void startFullEditActivity(String filePath) {
        UserAppActivity.startFullEditActivity(this, filePath);
    }

    // Called from the non-UI thread
    private void updateList() {
        handler.removeMessages(0);
        handler.sendEmptyMessageDelayed(0, 300);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public Drawable getFileIcon(String dirPath,
                                FileRecord record) {
        if (record == null) {
            return null;
        }
        // APK file
        if (!record.isDir && record.fileName.endsWith(".apk")) {
            final String path = dirPath + "/" + record.fileName;
            final ApkInfoParser.AppInfo info = this.apkIconCache.get(path);
            if (info != null) {
                return info.icon;
            }

            parseThread.addApk(path);

            return ContextCompat.getDrawable(this, R.drawable.round_android_24);
        }
        return null;
    }

    @Override
    public String getDetail1(String dirPath,
                             @NonNull FileRecord record) {
        if (!record.isDir && record.fileName.endsWith(".apk")) {
            final String path = dirPath + "/" + record.fileName;
            final ApkInfoParser.AppInfo info = this.apkIconCache.get(path);
            if (info != null) {
                return info.label;
            } else {
                return "";
            }
        }
        return null;
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.search_button) {
            if (foderWrapper != null) {
                final AppCompatEditText et = this.findViewById(R.id.keyword_edit);
                final String keyword = et.getText().toString();
                final String currentFolder = this.foderWrapper.getAdapter().getData(null);
                final Intent intent = new Intent(this, ApkSearchActivity.class);
                ActivityUtils.attachParam(intent, "Keyword", keyword);
                ActivityUtils.attachParam(intent, "Path", currentFolder);
                this.startActivity(intent);
            }
        }
    }

    // Simple edit or full edit clicked
    @Override
    public void editModeSelected(int mode, String extraStr) {

        Intent intent = null;
        switch (mode) {
            case EditModeView.SIMPLE_EDIT:
                intent = new Intent(this, SimpleEditActivity.class);
                break;
            case EditModeView.FULL_EDIT:
                startFullEditActivity(extraStr);
                return;
            case EditModeView.COMMON_EDIT:
                intent = new Intent(this, CommonEditActivity.class);
                break;
            case EditModeView.XML_FILE_EDIT:
                intent = new Intent(this, AxmlEditActivity.class);
                break;
        }

        if (intent != null) {
            ActivityUtils.attachParam(intent, "apkPath", extraStr);
            startActivity(intent);
        }
    }

    @Override
    public void updateFileList(String path) {
        foderWrapper.getAdapter().openDirectory(foderWrapper.getAdapter().getData(null));
    }

    class ApkParseThread extends Thread {
        private final List<String> apkList = new LinkedList<>();
        private boolean bStop = false;

        public void addApk(String path) {
            synchronized (apkList) {
                apkList.add(path);
                apkList.notify();
            }
        }

        public void stopParse() {
            bStop = true;
            synchronized (apkList) {
                apkList.notify();
            }
        }

        @Override
        public void run() {
            final ApkInfoParser apkParser = new ApkInfoParser();

            while (!bStop) {
                String path = null;
                synchronized (apkList) {
                    if (apkList.isEmpty()) {
                        try {
                            apkList.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (!apkList.isEmpty()) {
                        path = apkList.remove(0);
                    }
                }

                if (path == null) {
                    continue;
                }

                ApkInfoParser.AppInfo info = null;
                try {
                    info = apkParser.parse(FileListActivity.this, path);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                if (info != null) {
                    apkIconCache.put(path, info);
                    updateList();
                }
            }
        }
    }
}
