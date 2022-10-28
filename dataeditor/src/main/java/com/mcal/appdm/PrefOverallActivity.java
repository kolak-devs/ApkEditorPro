package com.mcal.appdm;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mcal.androlib.util.OpenFiles;
import com.mcal.appdm.base.R;
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PrefOverallActivity extends CustomizedLangActivity implements OnClickListener {

    protected static final int DETAIL_ACTIVITY_REQUEST_CODE = 1001;
    // File list in full path
    // private List<String> xmlFileList = new ArrayList<String>();
    // private List<String> dbFileList = new ArrayList<String>();
    private final List<StringPair> xmlFilePairs = new ArrayList<>();
    private final List<StringPair> dbFilePairs = new ArrayList<>();
    // Stage: -1, scanning not finished
    // 0, failed
    // 1, succeed
    int stage = -1;
    String errMsg;
    int curTabIndex = 0;
    private String packagePath;
    private ScanThread thread;
    private ListView prefListView;
    private ListView dbListView;
    private ListView appInfoListView;
    private ListView fileListView;
    private LinearLayout scanningLayout;
    private ProgressBar progressBar;
    private TextView tipTv;
    private PackageManager pm;
    private ApplicationInfo applicationInfo;
    private PackageInfo packageInfo;
    private String appName;
    private RootFileAdapter fileListAdapter;
    // Button in bottom
    private RadioButton infoBtn;
    private RadioButton prefBtn;
    private RadioButton dbBtn;
    private RadioButton fileBtn;
    private Drawable infoDrawable;
    private Drawable infoBlueDrawable;
    private Drawable prefDrawable;
    private Drawable prefBlueDrawable;
    private Drawable dbDrawable;
    private Drawable dbBlueDrawable;
    private Drawable fileDrawable;
    private Drawable fileBlueDrawable;
    // Is root mode or not
    private boolean isRootMode;

    // Show backup or not
    private boolean bShowBackup;

    private boolean prefModified = false;
    private long prefClickTime;
    private long prefReturnTime;
    private long createTime;
    private int prefClickedNum = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent intent = getIntent();
        setContentView(R.layout.appdm_activity_dataoverview);

        this.packagePath = ActivityHelper.getParam(intent, "packagePath");
        this.bShowBackup = ActivityHelper.getBoolParam(intent, "backup");
        try {
            this.pm = this.getPackageManager();
            this.applicationInfo = pm.getApplicationInfo(packagePath, 0);
            this.packageInfo = pm.getPackageInfo(packagePath, 0);

            this.createTime = System.currentTimeMillis();
            // The target app shares the same user id with me
            this.isRootMode = packageInfo.sharedUserId == null || !packageInfo.sharedUserId.equals(pm.getPackageInfo(getPackageName(), 0).sharedUserId);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        initUI();

        this.thread = new ScanThread(this);
        thread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Returned from detail activity
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DETAIL_ACTIVITY_REQUEST_CODE) {
            this.prefReturnTime = System.currentTimeMillis();
            // Log.d("DEBUG", "onActivityResult called, stayTime= "
            // + (prefReturnTime - prefClickTime) + ", resultCode=" +
            // resultCode);
            if (resultCode == 1) {
                this.prefModified = true;
            }
        }
    }

    private void initUI() {
        ImageView iv = (ImageView) this.findViewById(R.id.app_icon);
        iv.setImageDrawable(applicationInfo.loadIcon(pm));

        TextView tv = (TextView) this.findViewById(R.id.app_name);
        tv.setText(applicationInfo.loadLabel(pm));

        tv = (TextView) this.findViewById(R.id.app_pkgpath);
        tv.setText(applicationInfo.packageName);

        // Save Image
        Button backupBtn = (Button) this.findViewById(R.id.button_backup);
        if (bShowBackup) {
            backupBtn.setOnClickListener(this);
        } else {
            backupBtn.setVisibility(View.GONE);
        }

        // List view
        this.appInfoListView = (ListView) this.findViewById(R.id.appInfo_list);
        this.prefListView = (ListView) this.findViewById(R.id.preference_list);
        this.dbListView = (ListView) this.findViewById(R.id.database_list);
        this.fileListView = (ListView) this.findViewById(R.id.files_list);
        this.scanningLayout = (LinearLayout) this
                .findViewById(R.id.layout_scanning);
        this.progressBar = (ProgressBar) this.findViewById(R.id.progress_bar);
        this.tipTv = (TextView) this.findViewById(R.id.tv_tip);

        // App info
        List<BasicInfoItem> data = new ArrayList<>();
        initAppInfo(data);
        appInfoListView.setAdapter(new BasicInfoAdapter(this, data));

        // File list
        initFileListView();

        enableListSwitch();
    }

    private void initAppInfo(@NonNull List<BasicInfoItem> data) {
        Resources res = getResources();

        // App name
        {
            this.appName = applicationInfo.loadLabel(pm).toString();
            data.add(new BasicInfoItem(res.getString(R.string.appdm_app_name),
                    appName));
        }

        // Package name
        {
            data.add(new BasicInfoItem(
                    res.getString(R.string.appdm_package_name),
                    this.packagePath));
        }

        // Size
        File f = new File(applicationInfo.sourceDir);
        {
            // Map<String, String> values = new HashMap<String, String>();
            // values.put("NAME", "App Size");
            String strSize = FileHelperKt.getSizeDescription(f.length());
            data.add(new BasicInfoItem(res.getString(R.string.appdm_app_size),
                    strSize));
        }

        // Version
        String verInfo = res.getString(R.string.appdm_version_code) + ": "
                + packageInfo.versionCode + "\n"
                + res.getString(R.string.appdm_version_name) + ": "
                + packageInfo.versionName;
        data.add(new BasicInfoItem(res.getString(R.string.appdm_version),
                verInfo));

        // APK File Path
        BasicInfoItem pathItem = new BasicInfoItem(
                res.getString(R.string.appdm_apk_file_path),
                applicationInfo.sourceDir, res.getString(R.string.save),
                v -> saveTheApk());
        data.add(pathItem);

        // Apk Build Time
        try {
            ZipFile zipFile = new ZipFile(applicationInfo.sourceDir);
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
            long time = (codeTime < manifestTime ? manifestTime : codeTime);

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(time);
            String strTime = cal.getTime().toString();
            data.add(new BasicInfoItem(
                    res.getString(R.string.appdm_apk_build_time), strTime));

            zipFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Install Time
        {
            Calendar cal = Calendar.getInstance();
            long time = f.lastModified();
            cal.setTimeInMillis(time);

            String strTime = cal.getTime().toString();
            data.add(new BasicInfoItem(
                    res.getString(R.string.appdm_install_time), strTime));
        }

        // Publisher
        {
            String sig = SignatureInfoReader
                    .getSignature(applicationInfo.sourceDir);
            data.add(new BasicInfoItem(res.getString(R.string.appdm_signature),
                    sig));
        }

    }

    protected void saveTheApk() {
        new ApkSaveDialog(this, applicationInfo.sourceDir, this.appName).start();
    }

    public void setScanResult(final boolean succeed) {
        synchronized (this) {
            this.stage = (succeed ? 1 : 0);
            this.errMsg = thread.getErrorMsg();
        }

        this.runOnUiThread(() -> {
            PrefOverallActivity.this.findViewById(R.id.layout_scanning)
                    .setVisibility(View.INVISIBLE);

            if (succeed) {

                // Parse preference list
                String output = thread.getPrefList();
                if (output != null) {
                    String[] lines = output.split("\n");
                    for (String line : lines) {
                        if (line.endsWith(".xml")) {
                            String filePath = line;
                            int pos = line.lastIndexOf('/');
                            String filename = line.substring(pos + 1);
                            filename = filename.substring(0,
                                    filename.length() - 4);
                            xmlFilePairs.add(
                                    new StringPair(filename, filePath));
                        }
                    }
                }

                // Parse database list
                output = thread.getDbList();
                if (output != null) {
                    String[] lines = output.split("\n");
                    for (String line : lines) {
                        if (line.endsWith(".db")) {
                            String filePath = line;
                            int pos = line.lastIndexOf('/');
                            String filename = line.substring(pos + 1);
                            filename = filename.substring(0,
                                    filename.length() - 3);
                            dbFilePairs.add(
                                    new StringPair(filename, filePath));
                        }
                    }
                }

                initPrefListView();
                initDbListView();
                updateListView();

            } else {
                Toast.makeText(PrefOverallActivity.this, errMsg,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void updateListView() {
        switch (this.curTabIndex) {
            case 0:
                drawAppInfoListView();
                break;
            case 1:
                drawPrefListView();
                break;
            case 2:
                drawDbListView();
                break;
        }
    }

    @SuppressWarnings("deprecation")
    private void enableListSwitch() {

        Resources res = getResources();
        this.infoDrawable = res.getDrawable(R.drawable.round_info_24);
        this.infoBlueDrawable = res.getDrawable(R.drawable.round_info_blue_24);
        this.prefDrawable = res.getDrawable(R.drawable.round_settings_suggest_24);
        this.prefBlueDrawable = res.getDrawable(R.drawable.round_settings_suggest_blue_24);
        this.dbDrawable = res.getDrawable(R.drawable.round_storage_24);
        this.dbBlueDrawable = res.getDrawable(R.drawable.round_storage_blue_24);
        this.fileDrawable = res.getDrawable(R.drawable.round_folder_24);
        this.fileBlueDrawable = res.getDrawable(R.drawable.round_folder_blue_24);

        this.infoBtn = (RadioButton) this.findViewById(R.id.tab_appinfo);
        this.prefBtn = (RadioButton) this.findViewById(R.id.tab_preference);
        this.dbBtn = (RadioButton) this.findViewById(R.id.tab_database);
        this.fileBtn = (RadioButton) this.findViewById(R.id.tab_files);

        infoBtn.setOnClickListener(v -> {
            curTabIndex = 0;

            drawAppInfoListView();

            infoBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    infoBlueDrawable, null, null);
            prefBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    prefDrawable, null, null);
            dbBtn.setCompoundDrawablesWithIntrinsicBounds(null, dbDrawable,
                    null, null);
            fileBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    fileDrawable, null, null);
        });

        prefBtn.setOnClickListener(v -> {
            curTabIndex = 1;

            drawPrefListView();

            infoBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    infoDrawable, null, null);
            prefBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    prefBlueDrawable, null, null);
            dbBtn.setCompoundDrawablesWithIntrinsicBounds(null, dbDrawable,
                    null, null);
            fileBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    fileDrawable, null, null);
        });

        dbBtn.setOnClickListener(v -> {
            curTabIndex = 2;

            drawDbListView();

            infoBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    infoDrawable, null, null);
            prefBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    prefDrawable, null, null);
            dbBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    dbBlueDrawable, null, null);
            fileBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    fileDrawable, null, null);
        });

        fileBtn.setOnClickListener(v -> {
            curTabIndex = 3;

            drawFileListView();

            infoBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    infoDrawable, null, null);
            prefBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    prefDrawable, null, null);
            dbBtn.setCompoundDrawablesWithIntrinsicBounds(null, dbDrawable,
                    null, null);
            fileBtn.setCompoundDrawablesWithIntrinsicBounds(null,
                    fileBlueDrawable, null, null);
        });
    }

    protected void drawAppInfoListView() {
        appInfoListView.setVisibility(View.VISIBLE);
        prefListView.setVisibility(View.INVISIBLE);
        dbListView.setVisibility(View.INVISIBLE);
        fileListView.setVisibility(View.INVISIBLE);
        scanningLayout.setVisibility(View.INVISIBLE);
    }

    protected void drawPrefListView() {
        synchronized (this) {
            switch (this.stage) {
                case -1:
                    prefListView.setVisibility(View.INVISIBLE);
                    scanningLayout.setVisibility(View.VISIBLE);
                    break;
                case 0:
                    prefListView.setVisibility(View.INVISIBLE);
                    scanningLayout.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    tipTv.setText(PrefOverallActivity.this.errMsg);
                    break;
                case 1:
                    scanningLayout.setVisibility(View.INVISIBLE);
                    prefListView.setVisibility(View.VISIBLE);
                    break;
            }
        }
        appInfoListView.setVisibility(View.INVISIBLE);
        dbListView.setVisibility(View.INVISIBLE);
        fileListView.setVisibility(View.INVISIBLE);
    }

    protected void drawDbListView() {
        synchronized (this) {
            switch (this.stage) {
                case -1:
                    dbListView.setVisibility(View.INVISIBLE);
                    scanningLayout.setVisibility(View.VISIBLE);
                    break;
                case 0:
                    dbListView.setVisibility(View.INVISIBLE);
                    scanningLayout.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    tipTv.setText(PrefOverallActivity.this.errMsg);
                    break;
                case 1:
                    scanningLayout.setVisibility(View.INVISIBLE);
                    dbListView.setVisibility(View.VISIBLE);
                    break;
            }
        }
        appInfoListView.setVisibility(View.INVISIBLE);
        prefListView.setVisibility(View.INVISIBLE);
        fileListView.setVisibility(View.INVISIBLE);
    }

    protected void drawFileListView() {
        scanningLayout.setVisibility(View.INVISIBLE);

        appInfoListView.setVisibility(View.INVISIBLE);
        prefListView.setVisibility(View.INVISIBLE);
        dbListView.setVisibility(View.INVISIBLE);
        fileListView.setVisibility(View.VISIBLE);
    }

    private void initPrefListView() {
        prefListView.setAdapter(
                new NameAndPathAdapter(this, this.xmlFilePairs));
        prefListView.setOnItemClickListener((arg0, arg1, position, arg3) -> {
            PrefOverallActivity.this.prefClickTime = System
                    .currentTimeMillis();
            PrefOverallActivity.this.prefClickedNum += 1;

            Intent intent = new Intent(PrefOverallActivity.this,
                    PrefDetailActivity.class);
            ActivityHelper.attachParam(intent, "appName",
                    (String) applicationInfo.loadLabel(pm));
            ActivityHelper.attachParam(intent, "xmlFilePath",
                    xmlFilePairs.get(position).second);
            ActivityHelper.attachParam(intent, "packagePath",
                    PrefOverallActivity.this.packagePath);
            ActivityHelper.attachBoolParam(intent, "isRootMode",
                    PrefOverallActivity.this.isRootMode);
            startActivityForResult(intent, DETAIL_ACTIVITY_REQUEST_CODE);
        });
    }

    private void initDbListView() {
        dbListView.setAdapter(
                new NameAndPathAdapter(this, this.dbFilePairs));
        dbListView.setOnItemClickListener((arg0, arg1, position, arg3) -> {
            Intent intent = new Intent(PrefOverallActivity.this,
                    com.mcal.sqliteutil.SqliteTableListActivity.class);
            ActivityHelper.attachParam(intent, "dbFilePath",
                    dbFilePairs.get(position).second);
            ActivityHelper.attachParam(intent, "isRootMode",
                    (isRootMode ? "true" : "false"));
            startActivity(intent);
        });
    }

    private void initFileListView() {
        String curPath = getFilesDir().getPath();
        String packageName = getPackageName();
        int position = curPath.indexOf(packageName);
        String rootPath = curPath.substring(0, position);

        this.fileListAdapter = new RootFileAdapter(this,
                rootPath + packagePath + "/files", this.isRootMode);
        fileListView.setAdapter(fileListAdapter);
        fileListView.setOnItemClickListener((arg0, arg1, position1, arg3) -> fileItemClicked(position1));

        // Long click listener
        fileListView.setOnItemLongClickListener((parent, view, position12, id) -> {
            List<FileRecord> records = new ArrayList<FileRecord>();
            final String curDir = fileListAdapter.getData(records);
            final FileRecord rec = records.get(position12);
            if (rec.isDir) {
                return true;
            }

            parent.setOnCreateContextMenuListener(
                    new OnCreateContextMenuListener() {
                        public void onCreateContextMenu(ContextMenu menu,
                                                        View v, ContextMenuInfo menuInfo) {
                            // Open in Editor
                            MenuItem item1 = menu.add(0, Menu.FIRST, 0,
                                    R.string.appdm_open_in_editor);
                            item1.setOnMenuItemClickListener(
                                    item -> {
                                        extractAndOpenEditor(
                                                curDir + "/" + rec.fileName);
                                        return true;
                                    });
                        }
                    });
            return false;
        });
    }

    protected void fileItemClicked(int position) {
        List<FileRecord> records = new ArrayList<>();
        String curDir = fileListAdapter.getData(records);
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
                                List<FileRecord> subFiles = fileListAdapter
                                        .listFiles(dirPath, true);
                                Collections.sort(subFiles,
                                        new FilenameComparator());
                                if (subFiles != null) {
                                    fileListAdapter.updateList(dirPath,
                                            subFiles);
                                }
                            }

                            @Override
                            public void afterProcess() {
                            }
                        }, -1).show();
            } else {
                String filepath = curDir + "/" + rec.fileName;
                extractAndOpenEditor(filepath);
                //extractAndTryToOpen(filepath);
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
        String tempDir = ScopedStorage.getTempDir().getPath();
        String tmpFilePath = tempDir + "/_work"
                + (postfix != null ? postfix : "");

        CommandInterface rc = createCommandRunner();
        String strCmd = "cp";
        File bin = ScopedStorage.getMyCp();
        if (bin.exists()) {
            strCmd = bin.getPath();
        }
        boolean copyRet = rc.runCommand(
                String.format(strCmd + " \"%s\" %s", filePath, tmpFilePath),
                null, 2000, false);
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
                            Intent intent = TextEditor.getSoraEditor(PrefOverallActivity.this, tmpFilePath, filePath, isRootMode, new int[]{R.string.appdm_file_too_big,
                                    R.string.appdm_file_saved,
                                    R.string.appdm_not_found});
                            startActivityForResult(intent, 1000);
                        } else {
                            Toast.makeText(PrefOverallActivity.this,
                                            "Failed to open the file.", Toast.LENGTH_SHORT)
                                    .show();
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
                            OpenFiles
                                    .openFile(PrefOverallActivity.this, tmpFilePath);
                        } else {
                            Toast.makeText(PrefOverallActivity.this,
                                            "Failed to open the file.", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                }, -1).show();
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        // As backup is hidden, not implemented yet!
        if (id == R.id.button_backup) {
        }
    }

    // Support root mode and non-root mode
    protected CommandInterface createCommandRunner() {
        if (this.isRootMode) {
            return new RootCommand();
        } else {
            return new CommandRunner();
        }
    }

    static class MyFilter implements FilenameFilter {
        private final String type;

        public MyFilter(String type) {
            this.type = type;
        }

        @Override
        public boolean accept(File dir, @NonNull String name) {
            return name.endsWith(type);
        }
    }

    static class ScanThread extends Thread {
        private final String packagePath;
        WeakReference<PrefOverallActivity> activityRef;
        private String errMsg;

        // Record the output returned by ls
        private String prefOutput;
        private String dbOutput;

        public ScanThread(@NonNull PrefOverallActivity activity) {
            this.packagePath = activity.packagePath;
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            boolean succeed = scanXmlFiles();
            PrefOverallActivity activity = activityRef.get();
            if (activity != null) {
                activity.setScanResult(succeed);
            }
        }

        private boolean scanXmlFiles() {
            PrefOverallActivity activity = activityRef.get();
            if (activity == null) {
                return false;
            }

            if (!FileHelperKt.exist()) {
                this.errMsg = "Can not find SD card!";
                return false;
            }

            String curPath = activity.getFilesDir().getPath();
            String packageName = activity.getPackageName();
            int position = curPath.indexOf(packageName);
            if (position == -1) {
                this.errMsg = "Can not find data path!";
                return false;
            }

            String rootPath = curPath.substring(0, position);

            if (activity.isRootMode) {
                String prefPath = rootPath + packagePath
                        + "/shared_prefs/*.xml";
                String dbPath = rootPath + packagePath + "/databases/*.db";

                CommandInterface rc = activity.createCommandRunner();
                String strCommand = String.format("ls %s", prefPath);
                if (rc.runCommand(strCommand, null, 5000)) {
                    this.prefOutput = rc.getStdOut();
                } else {
                    errMsg = "Can not get access to read files!";
                    return false;
                }

                if (rc.runCommand(String.format("ls %s", dbPath), null, 5000)) {
                    this.dbOutput = rc.getStdOut();
                } else {
                    errMsg = "Can not get access to read files!";
                    return false;
                }
            }

            // For the non-root mode, directly list directory
            // Still DO NOT know why ls will fail
            else {
                File dir = new File(rootPath + packagePath + "/shared_prefs");
                File[] files = dir.listFiles();
                if (files != null) {
                    StringBuffer sb = new StringBuffer();
                    for (File f : files) {
                        String path = f.getAbsolutePath();
                        sb.append(path);
                        sb.append("\n");
                    }
                    this.prefOutput = sb.toString();
                }

                dir = new File(rootPath + packagePath + "/databases");
                files = dir.listFiles();
                if (files != null) {
                    StringBuffer sb = new StringBuffer();
                    for (File f : files) {
                        String path = f.getAbsolutePath();
                        sb.append(path);
                        sb.append("\n");
                    }
                    this.dbOutput = sb.toString();
                }
            }

            return true;
        }

        public String getPrefList() {
            return prefOutput;
        }

        public String getDbList() {
            return dbOutput;
        }

        public String getErrorMsg() {
            return errMsg;
        }
    }
}
