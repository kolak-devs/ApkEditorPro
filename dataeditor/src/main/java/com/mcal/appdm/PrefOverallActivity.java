package com.mcal.appdm;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mcal.androlib.util.OpenFiles;
import com.mcal.appdm.base.R;
import com.mcal.appdm.base.databinding.AppdmActivityDataoverviewBinding;
import com.mcal.appdm.utils.SignatureInfoReader;
import com.mcal.appdm.utils.StringPair;
import com.mcal.common.activities.CustomizedLangActivity;
import com.mcal.common.utils.ActivityHelper;
import com.mcal.common.utils.CommandInterface;
import com.mcal.common.utils.CommandRunner;
import com.mcal.common.utils.FileHelperKt;
import com.mcal.common.utils.FileRecord;
import com.mcal.common.utils.FilenameComparator;
import com.mcal.common.utils.RootCommand;
import com.mcal.common.utils.ScopedStorage;
import com.mcal.common.view.ProgressDialog;
import com.mcal.editor.TextEditor;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PrefOverallActivity extends CustomizedLangActivity {

    protected static final int DETAIL_ACTIVITY_REQUEST_CODE = 1001;
    // File list in full path
    // private List<String> xmlFileList = new ArrayList<String>();
    // private List<String> dbFileList = new ArrayList<String>();
    private final List<StringPair> mXmlFilePairs = new ArrayList<>();
    private final List<StringPair> mDbFilePairs = new ArrayList<>();
    // Stage: -1, scanning not finished
    // 0, failed
    // 1, succeed
    int mStage = -1;
    String mErrorMessage;
    private String mPackagePath;
    private ScanThread mThread;
    private PackageManager mPackageManager;
    private ApplicationInfo mApplicationInfo;
    private PackageInfo mPackageInfo;
    private String mAppName;
    private RootFileAdapter mFileListAdapter;
    // Is root mode or not
    private boolean isRootMode;

    @NonNull
    private AppdmActivityDataoverviewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        binding = AppdmActivityDataoverviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mPackagePath = ActivityHelper.getParam(intent, "packagePath");
        try {
            mPackageManager = getPackageManager();
            mApplicationInfo = mPackageManager.getApplicationInfo(mPackagePath, 0);
            mPackageInfo = mPackageManager.getPackageInfo(mPackagePath, 0);
            // The target app shares the same user id with me
            isRootMode = mPackageInfo.sharedUserId == null || !mPackageInfo.sharedUserId.equals(mPackageManager.getPackageInfo(getPackageName(), 0).sharedUserId);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        initUI();

        mThread = new ScanThread(this);
        mThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initUI() {
        setupToolbar(R.id.toolbar,
                mApplicationInfo.loadLabel(mPackageManager).toString(),
                mApplicationInfo.packageName,
                mApplicationInfo.loadIcon(mPackageManager),
                true);

        // App info
        List<BasicInfoItem> data = new ArrayList<>();
        initAppInfo(data);
        binding.appInfoList.setAdapter(new BasicInfoAdapter(this, data));

        // File list
        initFileListView();

        enableListSwitch();
    }

    private void initAppInfo(@NonNull List<BasicInfoItem> data) {
        Resources res = getResources();

        // App name
        mAppName = mApplicationInfo.loadLabel(mPackageManager).toString();
        data.add(new BasicInfoItem(res.getString(R.string.appdm_app_name), mAppName));

        // Package name
        data.add(new BasicInfoItem(res.getString(R.string.appdm_package_name), mPackagePath));

        // Size
        File f = new File(mApplicationInfo.sourceDir);
        String strSize = FileHelperKt.getSizeDescription(f.length());
        data.add(new BasicInfoItem(res.getString(R.string.appdm_app_size), strSize));

        // Version
        String verInfo = res.getString(R.string.appdm_version_code) + ": " + mPackageInfo.versionCode + "\n" + res.getString(R.string.appdm_version_name) + ": " + mPackageInfo.versionName;
        data.add(new BasicInfoItem(res.getString(R.string.appdm_version), verInfo));

        // APK File Path
        BasicInfoItem pathItem = new BasicInfoItem(res.getString(R.string.appdm_apk_file_path), mApplicationInfo.sourceDir, res.getString(R.string.save), v -> saveTheApk());
        data.add(pathItem);

        // Apk Build Time
        try {
            ZipFile zipFile = new ZipFile(mApplicationInfo.sourceDir);
            ZipEntry manifestEntry = zipFile.getEntry("AndroidManifest.xml");
            ZipEntry codeEntry = zipFile.getEntry("classes.dex");
            long manifestTime = Long.MIN_VALUE;
            if (manifestEntry != null) {
                manifestTime = manifestEntry.getTime();
            }
            long codeTime = Long.MIN_VALUE;
            if (codeEntry != null) {
                codeTime = codeEntry.getTime();
            }
            long time = (Math.max(codeTime, manifestTime));

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(time);
            String strTime = cal.getTime().toString();
            data.add(new BasicInfoItem(res.getString(R.string.appdm_apk_build_time), strTime));

            zipFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Install Time
        Calendar cal = Calendar.getInstance();
        long time = f.lastModified();
        cal.setTimeInMillis(time);

        String strTime = cal.getTime().toString();
        data.add(new BasicInfoItem(res.getString(R.string.appdm_install_time), strTime));

        // Publisher
        String sig = SignatureInfoReader.getSignature(mApplicationInfo.sourceDir);
        data.add(new BasicInfoItem(res.getString(R.string.appdm_signature), sig));
    }

    protected void saveTheApk() {
        new ApkSaveDialog(this, mApplicationInfo.sourceDir, mAppName).start();
    }

    public void setScanResult(final boolean succeed) {
        synchronized (this) {
            mStage = (succeed ? 1 : 0);
            mErrorMessage = mThread.getErrorMsg();
        }

        runOnUiThread(() -> {
            findViewById(R.id.layout_scanning).setVisibility(View.INVISIBLE);
            if (succeed) {

                // Parse preference list
                String output = mThread.getPrefList();
                if (output != null) {
                    String[] lines = output.split("\n");
                    for (String line : lines) {
                        if (line.endsWith(".xml")) {
                            String filePath = line;
                            int pos = line.lastIndexOf('/');
                            String filename = line.substring(pos + 1);
                            filename = filename.substring(0, filename.length() - 4);
                            mXmlFilePairs.add(new StringPair(filename, filePath));
                        }
                    }
                }

                // Parse database list
                output = mThread.getDbList();
                if (output != null) {
                    String[] lines = output.split("\n");
                    for (String line : lines) {
                        if (line.endsWith(".db")) {
                            String filePath = line;
                            int pos = line.lastIndexOf('/');
                            String filename = line.substring(pos + 1);
                            filename = filename.substring(0,
                                    filename.length() - 3);
                            mDbFilePairs.add(
                                    new StringPair(filename, filePath));
                        }
                    }
                }

                initPrefListView();
                initDbListView();
            } else {
                Toast.makeText(PrefOverallActivity.this, mErrorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void enableListSwitch() {
        binding.mainRadio.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.tab_appinfo) {
                drawAppInfoListView();
                return true;
            } else if (itemId == R.id.tab_preference) {
                drawPrefListView();
                return true;
            } else if (itemId == R.id.tab_database) {
                drawDbListView();
                return true;
            } else if (itemId == R.id.tab_files) {
                drawFileListView();
                return true;
            }
            return false;
        });
    }

    protected void drawAppInfoListView() {
        binding.appInfoList.setVisibility(View.VISIBLE);
        binding.preferenceList.setVisibility(View.INVISIBLE);
        binding.databaseList.setVisibility(View.INVISIBLE);
        binding.filesList.setVisibility(View.INVISIBLE);
        binding.layoutScanning.setVisibility(View.INVISIBLE);
    }

    protected void drawPrefListView() {
        synchronized (this) {
            switch (mStage) {
                case -1:
                    binding.preferenceList.setVisibility(View.INVISIBLE);
                    binding.layoutScanning.setVisibility(View.VISIBLE);
                    break;
                case 0:
                    binding.preferenceList.setVisibility(View.INVISIBLE);
                    binding.layoutScanning.setVisibility(View.VISIBLE);
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvTip.setText(mErrorMessage);
                    break;
                case 1:
                    binding.layoutScanning.setVisibility(View.INVISIBLE);
                    binding.preferenceList.setVisibility(View.VISIBLE);
                    break;
            }
        }
        binding.appInfoList.setVisibility(View.INVISIBLE);
        binding.databaseList.setVisibility(View.INVISIBLE);
        binding.filesList.setVisibility(View.INVISIBLE);
    }

    protected void drawDbListView() {
        synchronized (this) {
            switch (this.mStage) {
                case -1:
                    binding.databaseList.setVisibility(View.INVISIBLE);
                    binding.layoutScanning.setVisibility(View.VISIBLE);
                    break;
                case 0:
                    binding.databaseList.setVisibility(View.INVISIBLE);
                    binding.layoutScanning.setVisibility(View.VISIBLE);
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvTip.setText(PrefOverallActivity.this.mErrorMessage);
                    break;
                case 1:
                    binding.layoutScanning.setVisibility(View.INVISIBLE);
                    binding.databaseList.setVisibility(View.VISIBLE);
                    break;
            }
        }
        binding.appInfoList.setVisibility(View.INVISIBLE);
        binding.preferenceList.setVisibility(View.INVISIBLE);
        binding.filesList.setVisibility(View.INVISIBLE);
    }

    protected void drawFileListView() {
        binding.layoutScanning.setVisibility(View.INVISIBLE);

        binding.appInfoList.setVisibility(View.INVISIBLE);
        binding.preferenceList.setVisibility(View.INVISIBLE);
        binding.databaseList.setVisibility(View.INVISIBLE);
        binding.filesList.setVisibility(View.VISIBLE);
    }

    private void initPrefListView() {
        binding.preferenceList.setAdapter(new NameAndPathAdapter(this, this.mXmlFilePairs));
        binding.preferenceList.setOnItemClickListener((arg0, arg1, position, arg3) -> {
            Intent intent = new Intent(PrefOverallActivity.this, PrefDetailActivity.class);
            ActivityHelper.attachParam(intent, "appName", (String) mApplicationInfo.loadLabel(mPackageManager));
            ActivityHelper.attachParam(intent, "xmlFilePath", mXmlFilePairs.get(position).second);
            ActivityHelper.attachParam(intent, "packagePath", PrefOverallActivity.this.mPackagePath);
            ActivityHelper.attachBoolParam(intent, "isRootMode", PrefOverallActivity.this.isRootMode);
            startActivityForResult(intent, DETAIL_ACTIVITY_REQUEST_CODE);
        });
    }

    private void initDbListView() {
        binding.databaseList.setAdapter(new NameAndPathAdapter(this, this.mDbFilePairs));
        binding.databaseList.setOnItemClickListener((arg0, arg1, position, arg3) -> {
            Intent intent = new Intent(PrefOverallActivity.this, com.mcal.sqliteutil.SqliteTableListActivity.class);
            ActivityHelper.attachParam(intent, "dbFilePath", mDbFilePairs.get(position).second);
            ActivityHelper.attachParam(intent, "isRootMode", (isRootMode ? "true" : "false"));
            startActivity(intent);
        });
    }

    private void initFileListView() {
        String curPath = getFilesDir().getPath();
        String packageName = getPackageName();
        int position = curPath.indexOf(packageName);
        String rootPath = curPath.substring(0, position);

        mFileListAdapter = new RootFileAdapter(this, rootPath + mPackagePath + "/files", this.isRootMode);
        binding.filesList.setAdapter(mFileListAdapter);
        binding.filesList.setOnItemClickListener((arg0, arg1, position1, arg3) -> fileItemClicked(position1));

        // Long click listener
        binding.filesList.setOnItemLongClickListener((parent, view, position12, id) -> {
            final List<FileRecord> records = new ArrayList<FileRecord>();
            final String curDir = mFileListAdapter.getData(records);
            final FileRecord rec = records.get(position12);
            if (rec.isDir) {
                return true;
            }

            parent.setOnCreateContextMenuListener(
                    (menu, v, menuInfo) -> {
                        // Open in Editor
                        MenuItem item1 = menu.add(0, Menu.FIRST, 0, R.string.appdm_open_in_editor);
                        item1.setOnMenuItemClickListener(
                                item -> {
                                    extractAndOpenEditor(curDir + "/" + rec.fileName);
                                    return true;
                                });
                    });
            return false;
        });
    }

    protected void fileItemClicked(int position) {
        List<FileRecord> records = new ArrayList<>();
        String curDir = mFileListAdapter.getData(records);
        if (position < records.size()) {
            FileRecord rec = records.get(position);
            if (rec.isDir) {
                // Open the sub directory
                final String dirPath = getSubDirectory(curDir, rec.fileName);
                new ProgressDialog(
                        PrefOverallActivity.this,
                        "", "Working…", false,
                        new ProgressDialog.ProcessingInterface() {
                            @SuppressWarnings("unchecked")
                            @Override
                            public void process() {
                                List<FileRecord> subFiles = mFileListAdapter.listFiles(dirPath, true);
                                if (subFiles != null) {
                                    Collections.sort(subFiles, new FilenameComparator());
                                    mFileListAdapter.updateList(dirPath, subFiles);
                                }
                            }

                            @Override
                            public void afterProcess() {
                            }
                        }, -1).show();
            } else {
                String filepath = curDir + "/" + rec.fileName;
                extractAndOpenEditor(filepath);
            }
        }
    }

    @NonNull
    private String getSubDirectory(String curDir, String name) {
        if ("..".equals(name)) {
            int pos = curDir.lastIndexOf('/');
            if (pos != -1) {
                return curDir.substring(0, pos);
            } else {
                return curDir;
            }
        } else {
            return curDir + "/" + name;
        }
    }

    // Return the target file path
    // Return null if failed
    @Nullable
    private String copyFileBySu(@NonNull String filePath) {
        String postfix = null;
        int pos = filePath.lastIndexOf('.');
        if (pos != -1) {
            postfix = filePath.substring(pos);
            if (postfix.contains("/")) { // not the real postfix
                postfix = null;
            }
        }
        final String tempDir = ScopedStorage.getTempDir().getPath();
        final String tmpFilePath = tempDir + "/_work" + (postfix != null ? postfix : "");

        final CommandInterface rc = createCommandRunner();
        String strCmd = "cp";
        final File bin = ScopedStorage.getMyCp();
        if (bin.exists()) {
            strCmd = bin.getPath();
        }
        final boolean copyRet = rc.runCommand(String.format(strCmd + " \"%s\" %s", filePath, tmpFilePath), null, 2000, false);
        if (copyRet) {
            return tmpFilePath;
        } else {
            return null;
        }
    }

    private void extractAndOpenEditor(final String filePath) {
        new ProgressDialog(this, "", "Working…", false,
                new ProgressDialog.ProcessingInterface() {
                    String tmpFilePath = null;

                    @Override
                    public void process() {
                        tmpFilePath = copyFileBySu(filePath);
                    }

                    @Override
                    public void afterProcess() {
                        if (tmpFilePath != null) {
                            // Open the editor
                            Intent intent = TextEditor.getSoraEditor(PrefOverallActivity.this, tmpFilePath, filePath, isRootMode, new int[]{
                                    R.string.appdm_file_too_big,
                                    R.string.appdm_file_saved,
                                    R.string.appdm_not_found});
                            startActivityForResult(intent, 1000);
                        } else {
                            Toast.makeText(PrefOverallActivity.this, "Failed to open the file.", Toast.LENGTH_SHORT).show();
                        }
                    }

                }, -1).show();
    }

    // Extract and try to open it with external apps
    private void extractAndTryToOpen(final String filePath) {
        new ProgressDialog(this, "", "Working…", false,
                new ProgressDialog.ProcessingInterface() {
                    String tmpFilePath = null;

                    @Override
                    public void process() {
                        tmpFilePath = copyFileBySu(filePath);
                    }

                    @Override
                    public void afterProcess() {
                        if (tmpFilePath != null) {
                            OpenFiles.openFile(PrefOverallActivity.this, tmpFilePath);
                        } else {
                            Toast.makeText(PrefOverallActivity.this, "Failed to open the file.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, -1).show();
    }

    // Support root mode and non-root mode
    protected CommandInterface createCommandRunner() {
        if (isRootMode) {
            return new RootCommand();
        } else {
            return new CommandRunner();
        }
    }

    static class MyFilter implements FilenameFilter {
        private final String mType;

        public MyFilter(String type) {
            mType = type;
        }

        @Override
        public boolean accept(File dir, @NonNull String name) {
            return name.endsWith(mType);
        }
    }

    static class ScanThread extends Thread {
        private final String mPackagePath;
        private final PrefOverallActivity mActivity;
        private String mErrorMessage;

        // Record the output returned by ls
        private String mPrefOutput;
        private String mDbOutput;

        public ScanThread(@NonNull PrefOverallActivity activity) {
            mPackagePath = activity.mPackagePath;
            mActivity = activity;
        }

        @Override
        public void run() {
            boolean succeed = scanXmlFiles();
            mActivity.setScanResult(succeed);
        }

        private boolean scanXmlFiles() {
            PrefOverallActivity activity = mActivity;

            if (!FileHelperKt.exist()) {
                mErrorMessage = "Can not find SD card!";
                return false;
            }

            String curPath = activity.getFilesDir().getPath();
            String packageName = activity.getPackageName();
            int position = curPath.indexOf(packageName);
            if (position == -1) {
                mErrorMessage = "Can not find data path!";
                return false;
            }

            String rootPath = curPath.substring(0, position);

            if (activity.isRootMode) {
                String prefPath = rootPath + mPackagePath + "/shared_prefs/*.xml";
                String dbPath = rootPath + mPackagePath + "/databases/*.db";

                CommandInterface rc = activity.createCommandRunner();
                String strCommand = String.format("ls %s", prefPath);
                if (rc.runCommand(strCommand, null, 5000)) {
                    mPrefOutput = rc.getStdOut();
                } else {
                    mErrorMessage = "Can not get access to read files!";
                    return false;
                }

                if (rc.runCommand(String.format("ls %s", dbPath), null, 5000)) {
                    mDbOutput = rc.getStdOut();
                } else {
                    mErrorMessage = "Can not get access to read files!";
                    return false;
                }
            }

            // For the non-root mode, directly list directory
            // Still DO NOT know why ls will fail
            else {
                File dir = new File(rootPath + mPackagePath + "/shared_prefs");
                File[] files = dir.listFiles();
                if (files != null) {
                    StringBuilder sb = new StringBuilder();
                    for (File f : files) {
                        String path = f.getAbsolutePath();
                        sb.append(path);
                        sb.append("\n");
                    }
                    this.mPrefOutput = sb.toString();
                }

                dir = new File(rootPath + mPackagePath + "/databases");
                files = dir.listFiles();
                if (files != null) {
                    StringBuilder sb = new StringBuilder();
                    for (File f : files) {
                        String path = f.getAbsolutePath();
                        sb.append(path);
                        sb.append("\n");
                    }
                    mDbOutput = sb.toString();
                }
            }

            return true;
        }

        public String getPrefList() {
            return mPrefOutput;
        }

        public String getDbList() {
            return mDbOutput;
        }

        public String getErrorMsg() {
            return mErrorMessage;
        }
    }
}
