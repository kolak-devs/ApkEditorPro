package com.mcal.apkeditor.activities;

import static com.mcal.common.utils.FileHelperKt.copyFile;
import static com.mcal.common.utils.FileHelperKt.makeDir;
import static com.mcal.common.utils.FileHelperKt.readObjectFromFile;
import static com.mcal.common.utils.FileHelperKt.recursiveModifiedTime;
import static com.mcal.common.utils.FileHelperKt.reviseFileName;
import static com.mcal.common.utils.FileHelperKt.writeObjectToFile;
import static com.mcal.common.utils.FileHelperKt.writeToFile;
import static com.mcal.common.utils.PathHelperKt.replaceNameWith;
import static com.mcal.common.utils.StringHelperKt.getRandomString;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.androlib.LanguageMapping;
import com.mcal.apkeditor.ApkComposeService;
import com.mcal.apkeditor.ApkParseConsumer;
import com.mcal.apkeditor.ApkParseThread;
import com.mcal.apkeditor.BuildConfig;
import com.mcal.apkeditor.IGeneralCallback;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.ResListAdapter;
import com.mcal.apkeditor.ResNavigationMgr;
import com.mcal.apkeditor.ResSelectionChangeListener;
import com.mcal.apkeditor.SomethingChangedListener;
import com.mcal.apkeditor.StringListAdapter;
import com.mcal.apkeditor.adapters.IManifestChangeCallback;
import com.mcal.apkeditor.adapters.LineRecord;
import com.mcal.apkeditor.adapters.ManifestListAdapter;
import com.mcal.apkeditor.autocomplete.AutoCompleteAdapter;
import com.mcal.apkeditor.autocomplete.AutoCompleteTextView;
import com.mcal.apkeditor.databinding.ActivityApkinfoBinding;
import com.mcal.apkeditor.dialogs.AboutPluginDialog;
import com.mcal.apkeditor.dialogs.AddFolderDialog;
import com.mcal.apkeditor.dialogs.FileCopyDialog;
import com.mcal.apkeditor.dialogs.FileSelectDialog;
import com.mcal.apkeditor.dialogs.FileSelectDialog.IFileSelection;
import com.mcal.apkeditor.dialogs.LanguageSelectDialog;
import com.mcal.apkeditor.dialogs.PatchDialog;
import com.mcal.apkeditor.dialogs.RebuildConfirmDialog;
import com.mcal.apkeditor.dialogs.SearchFilenameDialog;
import com.mcal.apkeditor.dialogs.SearchTextDialog;
import com.mcal.apkeditor.dialogs.SmaliNoticeDialog;
import com.mcal.apkeditor.patch.interfaces.ApkInfoListener;
import com.mcal.apkeditor.smali.AsyncDecodeTask;
import com.mcal.apkeditor.smali.AsyncDecodeTask.IDecodeTaskCallback;
import com.mcal.apkeditor.translate.PossibleLanguages;
import com.mcal.apkeditor.translate.TranslateItem;
import com.mcal.apkeditor.ui.fulleditor.utils.SmaliUtilsKt;
import com.mcal.apkeditor.ui.fulleditor.utils.StringsUtils;
import com.mcal.common.activities.CustomizedLangActivity;
import com.mcal.common.data.Preferences;
import com.mcal.common.utils.ActivityHelper;
import com.mcal.common.utils.ApkInfoParser;
import com.mcal.common.utils.FileHelperKt;
import com.mcal.common.utils.FileRecord;
import com.mcal.common.utils.ScopedStorage;
import com.mcal.common.utils.ServiceUtil;
import com.mcal.common.utils.StringHelperKt;
import com.mcal.common.utils.TextFileReader;
import com.mcal.common.utils.UriUtils;
import com.mcal.common.utils.ZipHelper;
import com.mcal.common.view.ProgressDialog;
import com.mcal.editor.TextEditor;
import com.mcal.folderlist.util.OpenFiles;
import com.mcal.pngeditor.PhotoViewerActivity;

import org.jetbrains.annotations.Contract;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import brut.androlib.res.data.ResConfigFlags;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.value.ResReferenceValue;
import brut.androlib.res.data.value.ResScalarValue;
import brut.androlib.res.data.value.ResValue;
import brut.util.Duo;
import common.types.ActivityState;
import common.types.ProjectInfo;
import common.types.StringItem;

public class ApkInfoActivity extends CustomizedLangActivity
        implements OnItemClickListener, OnItemLongClickListener,
        IManifestChangeCallback, OnClickListener, IDecodeTaskCallback,
        OnLongClickListener, ApkParseConsumer, ResSelectionChangeListener, AddFolderDialog.AddFolderCallback, ApkInfoListener {
    // To edit/view a file before replacing
    public static final int RC_OPEN_BEFORE_REPLACE = 1001;
    // To edit/view a file in external app
    public static final int RC_OPEN_EXTERNAL = 1002;
    public static final int RC_REQUEST_PERMISSION = 1003;
    public static final int RC_SEARCH_MF = 2; // search manifest
    // Define the request code
    private static final int RC_FILE_EDITOR = 0;
    private static final int RC_COMPOSE = 1;
    private static final int RC_COLOR_EDITOR = 3;
    private static final int RC_TRANSLATE = 1000;
    private static final String TMP_EDITOR_FILE = "APKEDITOR.xcrhfvke";
    private final Stack<Duo<Integer, Integer>> resListPosition = new Stack<>();
    protected String apkPath;
    protected String decodeRootPath; // not ends with "/"
    protected ResNavigationMgr navigationMgr;
    protected ImageButton searchOptionImage;
    protected ImageButton searchOptionCase;
    // Current state
    protected boolean searchTextContent = true;
    protected boolean searchResSensitive = true;
    // Record all the file entry to zip entry
    // As images are dummy, we need this info to show original image
    protected Map<String, String> mFileEntry2ZipEntry;
    // Theme control
    protected int themeId;
    // Decode all files or not (all files means files include assets, libs, and unknown files)
    protected boolean isFullDecoding;
    ApkInfoParser.AppInfo apkInfo;
    // Is it OK to change to String type
    HashMap<String, ArrayList<StringItem>> allStringValues;
    Map<String, Map<String, String>> changedStringValues;
    ResListAdapter resListAdapter;
    ActivityApkinfoBinding binding;
    //HashMap<ResConfigFlags, ArrayList<StringItem>> allStringValues;
    //Map<ResConfigFlags, Map<String, String>> changedStringValues;
    //private ResConfigFlags curConfig = null; // config flag for string resource
    //private ArrayList<ResConfigFlags> langConfigList;
    private LinearLayout stringLayout;
    private StringListAdapter stringListAdapter;
    private String curConfig = null; // config flag for string resource
    private ArrayList<String> langConfigList;
    private LinearLayout resourceLayout;
    private ListView resourceList;
    private View resSearchLayout;
    private View resMenuLayout;
    private LinearLayout resNaviHeader;
    private View resSelectHeader;
    private TextView resSelectTip;
    private LinearLayout manifestLayout;
    private RecyclerView manifestRecyclerView;
    private ManifestListAdapter manifestListAdapter;
    private LinearLayout loadingLayout;
    private ImageButton patchMenu;
    private Button saveBtn;
    // APK parser
    private ApkParseThread parseThread;
    // Modified String/Manifest or not
    private boolean stringModified = false;
    // For auto translating support
    // private static final String TRANSLATE_DLG_CLASS =
    // "com.mcal.apkeditor.translate.TranslateDialog";
    // protected Object translageDlg;
    private boolean manifestModified = false;
    private boolean stringParsed = false;
    private boolean resourceParsed = false;
    private int curSelectedRadio = 0; // 0 - String, 1 - Resource, 2 - Manifest
    // For DEX decoding
    private View dexDecodeLayout;
    private Button dex2smaliImage;
    private ProgressBar decodeProgressBar;
    private View decodeResultLayout;
    private TextView decodeResultTitle;
    private TextView decodeResultDetail;
    private boolean dexDecoded = false;
    // Following 5 records are collected/renewed each time click at save button
    private HashMap<String, String> addedFiles;
    private HashMap<String, String> replacedFiles;
    private ArrayList<String> deletedFiles;
    private ArrayList<String> smaliFolders;
    private boolean resFileModified;
    // It may fail when first time to prepare the string, so record it
    // This is because the string may refer to the value of Android system
    private boolean bStringPrepared = false;
    // ONLY used for data recovering (the state in resource list adapter)
    private String resCurrentDir; // when rotate the screen, will save and recover from it
    private Map<String, String> res_addedFiles;
    private Map<String, String> res_replacedFiles;
    private Set<String> res_deletedFiles;
    // Auto complete adapters
    private AutoCompleteAdapter strKeywordAdapter;
    private AutoCompleteAdapter resKeywordAdapter;
    private AutoCompleteAdapter mfKeywordAdapter;
    // Used to notify dex decoded watcher
    private IGeneralCallback mDexDecodedCallback;
    // Params saved before activity launch
    private String savedParam_filePath;
    private String savedParam_extraStr;
    private SomethingChangedListener savedParam_listener;
    // When projectName != null, means opened from a project
    private String projectName;
    // Recorded before open a file in external editor
    private String filePathForExternal;
    private String entryNameForExternal;
    private long modifiedTimeBeforeOpen;
    private BottomNavigationView buttonBar;

    // prjDirectory not ends with '/'
    @Nullable
    public static ProjectInfo loadProject(String prjDirectory) {
        String versionPath = prjDirectory + "/.prj_version";
        String infoPath = prjDirectory + "/ae.prj";
        File versionFile = new File(versionPath);
        File prjInfoFile = new File(infoPath);
        if (versionFile.exists() && prjInfoFile.exists()) {
            try {
                TextFileReader reader = new TextFileReader(versionPath);
                int version = Integer.parseInt(reader.getContents());
                if (version == 1) {
                    return (ProjectInfo) readObjectFromFile(infoPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // filename does not contain ".apk"
    public static String createOutputPath(String inputPath, @NonNull String outputDir, String filename) {
        if (outputDir.endsWith("/")) {
            outputDir = outputDir.substring(0, outputDir.length() - 1);
        }

        String targetApkPath = outputDir + "/" + filename + ".apk";

        int position = inputPath.lastIndexOf('/');
        String inputDir = inputPath.substring(0, position);
        String inputName = inputPath.substring(position + 1);
        // Need to revise the output file name
        // signed -> signed2, signed2 -> signed3
        if (inputDir.equals(outputDir) && inputName.startsWith(filename)) {
            String strEnd = inputName.substring(filename.length());
            if (".apk".equals(strEnd)) {
                targetApkPath = outputDir + "/" + filename + "2.apk";
            } else if (strEnd.matches("[1-9][0-9]*\\.apk")) {
                String strNum = strEnd.substring(0, strEnd.length() - 4);
                try {
                    int idx = Integer.parseInt(strNum);
                    idx += 1;
                    targetApkPath = outputDir + "/" + filename + idx + ".apk";
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return targetApkPath;
    }

    public static boolean generalTranslatePluginExist(@NonNull Context ctx) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(null,
                "application/com.mcal.apkeditor-translate");
        PackageManager manager = ctx.getPackageManager();
        List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
        return (infos.size() > 0);
    }

    @Nullable
    public static ResConfigFlags getBestConfigFlags(@NonNull Set<ResConfigFlags> configs) {
        Set<String> qualifiers = new HashSet<>();
        for (ResConfigFlags configFlags : configs) {
            qualifiers.add(configFlags.getQualifiers());
        }
        String matches = getBestConfig(qualifiers);
        for (ResConfigFlags configFlags : configs) {
            if (configFlags.getQualifiers().equals(matches)) {
                return configFlags;
            }
        }
        return null;
    }

    // Get best configuration according to current locale
    public static String getBestConfig(@NonNull Set<String> configs) {
        String bestConfig = null;

        Locale locale = Locale.getDefault();
        String realLang = "-" + locale.getLanguage();
        String realCountry = locale.getCountry();
        String realQualifier = realLang + "-r" + realCountry;

        for (String quaifier : configs) {
            if (realQualifier.equals(quaifier)) {
                bestConfig = quaifier;
                break;
            }

            // Country is not set and lang is the same
            if (realLang.equals(quaifier)) {
                bestConfig = quaifier;
            }

            // Default
            else if (quaifier.equals("") && bestConfig == null) {
                bestConfig = quaifier;
            }
        }

        return bestConfig;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityApkinfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // If projectName is not null, means recover from a project
        projectName = ActivityHelper.getParam(getIntent(), "projectName");

        ProjectInfo prjInfo = null;
        if (projectName != null) {
            try {
                String prjRoot = makeDir(".projects").getPath();
                prjInfo = loadProject(prjRoot + File.separator + projectName);
                apkPath = prjInfo.apkPath;
                decodeRootPath = prjInfo.decodeRootPath;
            } catch (Exception e) {
                Toast.makeText(this, R.string.cannot_load_project_info, Toast.LENGTH_LONG).show();
                this.finish();
                return;
            }
        } else {
            // Get apk path from URI (open from third-party file explorer)
            Intent intent = getIntent();
            Uri uri = intent.getData();
            if (uri != null) {
                // Request for permission
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            RC_REQUEST_PERMISSION);
                }
                apkPath = UriUtils.getAbsolutePath(this, uri);
            }
            // Get it from Intent
            if (apkPath == null) {
                apkPath = ActivityHelper.getParam(getIntent(), "apkPath");
            }
            decodeRootPath = ActivityHelper.getParam(getIntent(), "decodeRootPath");
            if (decodeRootPath == null) {
                String decodeDir = FileHelperKt.getDecodeDirectory();
                if (decodeDir != null) {
                    decodeRootPath = decodeDir + "/decoded";
                } else {
                    File fileDir = this.getFilesDir();
                    String rootDirectory = fileDir.getAbsolutePath();
                    decodeRootPath = rootDirectory + "/decoded";
                }
            }
        }

        // Parse information from apk
        try {
            if (apkPath != null && !"".equals(apkPath)) {
                this.apkInfo = new ApkInfoParser().parse(this, apkPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.stringListAdapter = new StringListAdapter(this);
        initView();

        // If saved instance != null, do not need to parse any more
        if (savedInstanceState != null) {
            recoverFromBundle(savedInstanceState);
        } else if (projectName != null) {
            // For APK Parser
            if (BuildConfig.PARSER_ONLY) {
                loadParserProject();
            } else if (prjInfo != null && prjInfo.state != null) {
                recoverFromProject(prjInfo);
            } else {
                Toast.makeText(this, R.string.cannot_load_project_info, Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            this.isFullDecoding = ActivityHelper.getBoolParam(getIntent(), "isFullDecoding");
            this.parseThread = new ApkParseThread(this, this, apkPath, decodeRootPath, isFullDecoding);
            parseThread.start();
        }
    }

    // save to file, also include version file
    private boolean storeProject(String prjDirectory, ProjectInfo prjInfo) {
        String versionPath = prjDirectory + "/.prj_version";
        String infoPath = prjDirectory + "/ae.prj";
        try {
            writeToFile(versionPath, "1");
            return writeObjectToFile(infoPath, prjInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Always recover from a bundle
    private void recoverFromProject(@NonNull ProjectInfo prjInfo) {
        Bundle bundle = new Bundle();
        prjInfo.state.toBundle(bundle);
        recoverFromBundle(bundle);
    }

    // Async recovering
    private void recoverFromBundle(final Bundle savedInstanceState) {
        new ProgressDialog(this, "", "Working…", false,
                new ProgressDialog.ProcessingInterface() {
                    @Override
                    public void process() {
                        recoverData(savedInstanceState);
                    }

                    @Override
                    public void afterProcess() {
                        recoverView();
                    }
                }, -1).show();
    }

    @NonNull
    private ActivityState saveCurrentState(String workingPath) {
        return saveCurrentState(workingPath, false, null);
    }

    // workingPath: path where to save object files, ends with "/"
    // saveAll: save big files like all strings
    // projectDir: should move temp files to this project directory; null means do nothing
    @NonNull
    private ActivityState saveCurrentState(
            String workingPath, boolean saveAll, File projectDir) {
        ActivityState state = new ActivityState();

        // Save changed string values
        Map<String, Map<String, String>>
                changedValues = stringListAdapter.getChangedValues();
        if (changedValues != null && !changedValues.isEmpty()) {
            this.stringModified = true;
            String path = workingPath + File.separator + "changedStringValues";
            writeObjectToFile(path, changedValues);
            state.putString("changedStringValues_file", path);
        }

        // Save Resource file state (for all files decoding, do not need to save resource state)
        if (!isFullDecoding && resListAdapter != null) {
            String curDir = resListAdapter.getData(null);
            state.putString("res_current_dir", curDir);
            Map<String, String> added = resListAdapter.getAddedFiles();
            Map<String, String> replaced = resListAdapter.getReplacedFiles();
            Set<String> deleted = resListAdapter.getDeletedFiles();
            if (projectDir != null) {
                moveTempFile2ProjectDir(added, replaced, projectDir);
            }
            if (added != null && !added.isEmpty()) {
                String path = workingPath + File.separator + "res_added";
                writeObjectToFile(path, added);
                state.putString("res_added_file", path);
            }
            if (replaced != null && !replaced.isEmpty()) {
                String path = workingPath + File.separator + "res_replaced";
                writeObjectToFile(path, replaced);
                state.putString("res_replaced_file", path);
            }
            if (deleted != null && !deleted.isEmpty()) {
                String path = workingPath + File.separator + "res_deleted";
                writeObjectToFile(path, deleted);
                state.putString("res_deleted_file", path);
            }
        }

        // Do not do it any more as consumes too much time (in case of rotation)
        String path = workingPath + File.separator + "allStringValues";
        if (saveAll) {
            writeObjectToFile(path, allStringValues);
        }
        state.putString("allStringValues_file", path);

        path = workingPath + File.separator + "fileEntry2ZipEntry";
        if (saveAll && !isFullDecoding) { // when all files decoded, do not need to save the mapping
            writeObjectToFile(path, mFileEntry2ZipEntry);
        }
        state.putString("fileEntry2ZipEntry_file", path);

        state.putString("curConfig", curConfig);
        state.putSerializable("langConfigList", langConfigList);
        state.putBoolean("stringModified", stringModified);
        state.putBoolean("manifestModified", manifestModified);
        state.putBoolean("stringParsed", stringParsed);
        state.putBoolean("resourceParsed", resourceParsed);
        state.putBoolean("bStringPrepared", bStringPrepared);
        state.putBoolean("searchTextContent", searchTextContent);
        state.putBoolean("searchResSensitive", searchResSensitive);
        state.putInt("curSelectedRadio", curSelectedRadio);

        state.putBoolean("dex2smaliClicked", dexDecoded);

        state.putString("savedParam_extraStr", savedParam_extraStr);
        state.putString("savedParam_filePath", savedParam_filePath);

        state.putBoolean("isFullDecoding", isFullDecoding);

        return state;
    }

    // Move temporary files to project folder, so that it will always there even with cleanup
    private void moveTempFile2ProjectDir(Map<String, String> added,
                                         Map<String, String> replaced, File projectDir) {
        String tmpFolder = ScopedStorage.getTmpDir().getPath();
        if (added != null && !added.isEmpty()) {
            for (Map.Entry<String, String> entry : added.entrySet()) {
                String path = entry.getValue();
                if (path != null && path.startsWith(tmpFolder)) {
                    File oldFile = new File(path);
                    File newFile = new File(projectDir, oldFile.getName());
                    if (oldFile.renameTo(newFile)) {
                        entry.setValue(newFile.getPath());
                    }
                }
            }
        }
        if (replaced != null && !replaced.isEmpty()) {
            for (Map.Entry<String, String> entry : replaced.entrySet()) {
                String path = entry.getValue();
                if (path != null && path.startsWith(tmpFolder)) {
                    File oldFile = new File(path);
                    File newFile = new File(projectDir, oldFile.getName());
                    if (oldFile.renameTo(newFile)) {
                        entry.setValue(newFile.getPath());
                    }
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        try {
            String workingPath;
            if (projectName != null) {
                String prjRoot = makeDir(".projects").getPath();
                workingPath = prjRoot + File.separator + projectName;
            } else {
                workingPath = ScopedStorage.getTmpDir().getPath();
            }

            ActivityState state = saveCurrentState(workingPath);
            state.toBundle(savedInstanceState);
        } catch (Exception ignored) {
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    private void loadParserProject() {
        allStringValues = new HashMap<>();
        changedStringValues = null;
        mFileEntry2ZipEntry = null;

        curConfig = null;
        langConfigList = null;
        stringModified = false;
        manifestModified = false;
        stringParsed = true;
        resourceParsed = true;
        bStringPrepared = true;
        curSelectedRadio = 1;

        dexDecoded = false;
        isFullDecoding = true;

        recoverView();
    }

    private void recoverData(@NonNull Bundle savedInstanceState) {

        String path = savedInstanceState.getString("allStringValues_file");
        allStringValues = (HashMap) readObjectFromFile(path);

        path = savedInstanceState.getString("changedStringValues_file");
        if (path != null) {
            changedStringValues = (HashMap) readObjectFromFile(path);
            StringsUtils.mergeStrings(allStringValues, changedStringValues);
        }

        path = savedInstanceState.getString("fileEntry2ZipEntry_file");
        mFileEntry2ZipEntry = (HashMap) readObjectFromFile(path);

        curConfig = savedInstanceState.getString("curConfig");
        langConfigList = (ArrayList) savedInstanceState.getSerializable("langConfigList");
        stringModified = savedInstanceState.getBoolean("stringModified");
        manifestModified = savedInstanceState.getBoolean("manifestModified");
        stringParsed = savedInstanceState.getBoolean("stringParsed");
        resourceParsed = savedInstanceState.getBoolean("resourceParsed");
        bStringPrepared = savedInstanceState.getBoolean("bStringPrepared");
        searchTextContent = savedInstanceState.getBoolean("searchTextContent");
        searchResSensitive = savedInstanceState.getBoolean("searchResSensitive");
        curSelectedRadio = savedInstanceState.getInt("curSelectedRadio");

        // Recover state of ResListAdapter
        resCurrentDir = savedInstanceState.getString("res_current_dir");
        String filePath = savedInstanceState.getString("res_added_file");
        if (filePath != null) {
            res_addedFiles = (Map) readObjectFromFile(filePath);
        }
        filePath = savedInstanceState.getString("res_replaced_file");
        if (filePath != null) {
            res_replacedFiles = (Map) readObjectFromFile(filePath);
        }
        filePath = savedInstanceState.getString("res_deleted_file");
        if (filePath != null) {
            res_deletedFiles = (Set) readObjectFromFile(filePath);
        }

        dexDecoded = savedInstanceState.getBoolean("dex2smaliClicked");

        savedParam_extraStr = savedInstanceState.getString("savedParam_extraStr");
        savedParam_filePath = savedInstanceState.getString("savedParam_filePath");

        isFullDecoding = savedInstanceState.getBoolean("isFullDecoding");
    }

    private void recoverView() {
        showStringList();
        setupClickListener();
        showDecodedFileList();

        // DEX decoded or not
        if (dexDecoded) {
            dexDecodeLayout.setVisibility(View.GONE);
        }

        // Search text or not
        updateSearchOption();

        patchMenu.setVisibility(View.VISIBLE);
        if (!BuildConfig.PARSER_ONLY) {
            saveBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        boolean dealed = false;

        // In resource page
        if (curSelectedRadio == 1) {
            if (resListAdapter == null) {
                dealed = false;
            } else if (!resListAdapter.getCheckedItems().isEmpty()) {
                resListAdapter.checkAllItems(false);
                dealed = true;
            } else if (!resListAdapter.isInRootDir()) {
                String oldDir = resListAdapter.getData(null);
                int idx = oldDir.lastIndexOf('/');
                String targetPath = oldDir.substring(0, idx);

                resListAdapter.openDirectory(targetPath);
                navigationMgr.gotoDirectory(targetPath);

                // Recover from the old position
                try {
                    Duo<Integer, Integer> pos = resListPosition.pop();
                    resourceList.setSelectionFromTop(pos.m1, pos.m2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                dealed = true;
            }
        }

        if (dealed) {
            return;
        }

        MaterialAlertDialogBuilder dlg = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sure_to_exit);
        if (parseThread == null || !parseThread.isAlive()) {
            dlg.setItems(R.array.save_as_projects, (dialog, which) -> {
                switch (which) {
                    case 0: {
                        finish();
                        break;
                    }
                    case 1: {
                        dialog.cancel();
                        break;
                    }
                    case 2: {
                        saveAsProject();
                        break;
                    }
                }
            });
        } else {
            dlg.setPositiveButton(android.R.string.ok, (d, i) -> finish());
            dlg.setNegativeButton(android.R.string.cancel, null);
        }
        dlg.show();
    }

    // Save current decoding as project
    private void saveAsProject() {
        new ProgressDialog(this, "", "Working…", false,
                new ProgressDialog.ProcessingInterface() {
                    ActivityState state;
                    File targetDir;
                    File projectDir;

                    String errorMessage = null;

                    @Override
                    public void process() {
                        // Save to project files
                        String projectName = "UNKNOWN";
                        if (apkInfo != null) {
                            projectName = apkInfo.label;
                        }

                        String parentFolder;
                        try {
                            parentFolder = makeDir(".projects").getPath();
                        } catch (Exception e) {
                            errorMessage = String.format(getString(R.string.general_error), e.getMessage());
                            return;
                        }

                        // Locate the project folder
                        String projectFolder = parentFolder + File.separator + projectName;
                        projectDir = new File(projectFolder);
                        if (projectDir.exists()) {
                            projectDir = FileCopyDialog.getTargetNonExistFile(projectFolder, true);
                        }
                        boolean ret = projectDir.mkdirs();
                        if (!ret) {
                            errorMessage = getString(R.string.cannot_save_project);
                            return;
                        }

                        // Rename decoded folder (as currently always in a fixed directory)
                        String targetFolder = replaceNameWith(decodeRootPath, projectName);
                        targetDir = new File(targetFolder);
                        if (targetDir.exists()) {
                            targetDir = FileCopyDialog.getTargetNonExistFile(targetFolder, true);
                        }
                        ret = new File(decodeRootPath).renameTo(targetDir);
                        if (!ret) {
                            errorMessage = getString(R.string.cannot_rename_decode_folder);
                            return;
                        }

                        // For APK Parser, do not save the state
                        // Rename the file path as "decoded" changed to project name
                        resListAdapter.renamePathAsFolderRename(decodeRootPath, targetDir.getPath() + "/");

                        state = saveCurrentState(projectDir.getPath(), true, projectDir);
                        if (state == null) {
                            errorMessage = "Cannot save project state.";
                        }
                    }

                    @Override
                    public void afterProcess() {
                        if (errorMessage != null) {
                            Toast.makeText(ApkInfoActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            return;
                        }

                        // For APK Parser, do not save other information
                        if (BuildConfig.PARSER_ONLY) {
                            finish();
                        } else {
                            // Save project info
                            ProjectInfo prjInfo = new ProjectInfo();
                            prjInfo.state = state;
                            prjInfo.apkPath = apkPath;
                            prjInfo.decodeRootPath = targetDir.getPath();
                            boolean ret = storeProject(projectDir.getPath(), prjInfo);
                            if (!ret) {
                                Toast.makeText(ApkInfoActivity.this, R.string.cannot_save_project, Toast.LENGTH_LONG).show();
                            } else {
                                finish();
                            }
                        }
                    }
                }, -1).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_FILE_EDITOR: // TextEditNormalActivity
            case RC_COLOR_EDITOR: // ColorXmlActivity
                if (resultCode != 0) {
                    ArrayList<String> files = data.getStringArrayListExtra("modifiedFiles");
                    if (files != null) { // in batch mode
                        for (String file : files) {
                            dealWithModifiedFile(file, null);
                        }
                        break;
                    }

                    String filePath = data.getStringExtra("filePath");
                    String entryName = data.getStringExtra("extraString");

                    fileModifiedIncludeTempPath(filePath, entryName);
                }
                break;
            case RC_COMPOSE: // ApkComposeActivity
                // Exit when succeed
                if (resultCode == ApkComposeActivity.SUCCEED) {
                    this.finish();
                }
                break;
            case RC_SEARCH_MF: // Open manifest in a new window, manifest search
                if (resultCode != 0) {
                    setManifestModified(true);
                }
                break;
            case RC_TRANSLATE: // Return from auto translate activity
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    String strQualifier = bundle.getString("targetLanguageCode");
                    String path = bundle.getString("translatedList_file");
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    List<TranslateItem> items = (List) readObjectFromFile(path);
                    if (items != null && !items.isEmpty()) {
                        List<StringItem> valueList = new ArrayList<>();
                        for (TranslateItem item : items) {
                            StringItem si = new StringItem(
                                    item.name, item.translatedValue);
                            valueList.add(si);
                        }
                        try {
                            saveTranslatedLanguage(strQualifier, valueList);
                            String strFormat = getString(R.string.save_succeed_tip);
                            String msg = String.format(strFormat, valueList.size());
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
                break;
            // Return from external file editing activity (when replace a file)
            case RC_OPEN_BEFORE_REPLACE:
                resListAdapter.replaceFile(savedParam_extraStr, savedParam_filePath);
                // Only code for Manifest
                if ((decodeRootPath + "/AndroidManifest.xml").equals(savedParam_extraStr)) { // AndroidManifest
                    setManifestModified(true);
                }
                if (savedParam_listener != null) {
                    savedParam_listener.somethingChanged();
                }
                break;
            case RC_OPEN_EXTERNAL:
                long mt = new File(filePathForExternal).lastModified();
                if (mt > modifiedTimeBeforeOpen) {
                    fileModifiedIncludeTempPath(filePathForExternal, entryNameForExternal);
                }
                break;
            default:
                break;
        }
    }

    private void fileModifiedIncludeTempPath(@NonNull String filePath, String entryName) {
        // To get the string removed last type
        String pathRemovedType;
        String[] names = filePath.split("/");
        int pos = names[names.length - 1].lastIndexOf('.');
        if (pos != -1) {
            int len = names[names.length - 1].length() - pos;
            pathRemovedType = filePath.substring(0, filePath.length() - len);
        } else {
            pathRemovedType = filePath;
        }

        // It is a temporary file
        // Rename it to other names, as it will be overwritten
        if (pathRemovedType != null && pathRemovedType.endsWith("/" + TMP_EDITOR_FILE)) {
            File oldFile = new File(filePath);
            String newPath = pathRemovedType.substring(0, pathRemovedType.length() - TMP_EDITOR_FILE.length()) + getRandomString(8);
            File newFile = new File(newPath);
            if (oldFile.renameTo(newFile)) {
                filePath = newPath;
            } else {
                Log.w("DEBUG", "file rename error.");
            }
        }
        dealWithModifiedFile(filePath, entryName);
    }

    // Called in onActivityResult
    public void dealWithModifiedFile(String filePath, String entryName) {
        if (entryName == null) {
            entryName = filePath.substring(decodeRootPath.length() + 1);
        }

        resListAdapter.fileModified(entryName, filePath);
    }

    // The manifest has been changed
    // bInUiThread: the invoke thread is in UI thread or not
    public void setManifestModified(boolean bInUiThread) {
        manifestModified = true;
        if (bInUiThread) {
            manifestListAdapter.reload();
        } else {
            runOnUiThread(() -> manifestListAdapter.reload());
        }
    }

    private void initView() {
        binding.mainRadio.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            /**
             * Called when an item in the navigation menu is selected.
             *
             * @param item The selected item
             * @return true to display the item as the selected item and false if the item should not be
             * selected. Consider setting non-selectable items as disabled preemptively to make them
             * appear non-interactive.
             */
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.tab_string: {
                        stringRadioClicked();
                        return true;
                    }
                    case R.id.tab_resource: {
                        resRadioClicked();
                        return true;
                    }
                    case R.id.tab_manifest: {
                        manifestRadioClicked();
                        return true;
                    }
                }
                return false;
            }
        });

        ImageView apkIcon = binding.appIcon;
        TextView apkLabel = binding.appName;
        TextView apkPkgPath = binding.appPkgpath;


        findViewById(R.id.layout_search_mf).setVisibility(View.VISIBLE);
        setupMfSearch();

        stringLayout = findViewById(R.id.string_layout);
        ListView stringList = findViewById(R.id.string_list);
        resourceLayout = findViewById(R.id.resource_layout);
        resourceList = findViewById(R.id.resource_list);
        findViewById(R.id.menu_search_res).setOnClickListener(this);
        resSearchLayout = findViewById(R.id.res_search_layout);
        resMenuLayout = findViewById(R.id.res_menu_layout);
        HorizontalScrollView resNaviScrollView = findViewById(R.id.res_navi_scrollView);
        resNaviHeader = findViewById(R.id.res_header_navigation);
        resSelectHeader = findViewById(R.id.res_header_selection);
        navigationMgr = new ResNavigationMgr(this, decodeRootPath, resNaviHeader, resNaviScrollView);
        resSelectTip = findViewById(R.id.selection_tip);

        manifestLayout = findViewById(R.id.manifest_layout);
        manifestRecyclerView = findViewById(R.id.manifest_list);
        loadingLayout = findViewById(R.id.layout_loading);

        if (apkInfo != null) {
            apkIcon.setImageDrawable(apkInfo.icon);
            apkLabel.setText(apkInfo.label);
            apkPkgPath.setText(apkInfo.pkgName);
        } else {
            if (projectName != null) {
                apkIcon.setImageResource(R.drawable.round_android_24);
                apkLabel.setText(projectName);
            } else {
                apkIcon.setImageResource(R.drawable.round_error_24);
                apkLabel.setText("UNKNOWN");
            }
            apkPkgPath.setVisibility(View.GONE);
        }

        stringList.setAdapter(stringListAdapter);
        stringList.setOnItemClickListener(stringListAdapter);

        // DEX/Smali decoding
        dexDecodeLayout = findViewById(R.id.dex_decode_layout);
        if (Preferences.isDex2smaliEnabled()) {
            dex2smaliImage = findViewById(R.id.imageview_dex2smali);
            dex2smaliImage.setOnClickListener(this);
            dex2smaliImage.setOnLongClickListener(this);
            decodeProgressBar = findViewById(R.id.progressbar_dex2smali);
            decodeResultLayout = findViewById(R.id.decode_result_layout);
            decodeResultTitle = findViewById(R.id.decode_result_title);
            decodeResultDetail = findViewById(R.id.decode_result_detail);
            findViewById(R.id.down_arrow_container).setOnClickListener(this);
        } else {
            dexDecodeLayout.setVisibility(View.GONE);
        }

        // File search option
        searchOptionImage = findViewById(R.id.imageview_text_check);
        searchOptionCase = findViewById(R.id.imageview_insensitive_check);
        updateSearchOption();

        // keyword auto complete
        AutoCompleteTextView editView = findViewById(R.id.keyword_edit);
        strKeywordAdapter = new AutoCompleteAdapter(this, "string_keywords");
        editView.setAdapter(strKeywordAdapter);
        editView = findViewById(R.id.et_res_keyword);
        resKeywordAdapter = new AutoCompleteAdapter(this, "res_keywords");
        editView.setAdapter(resKeywordAdapter);
        editView = findViewById(R.id.mf_keyword);
        mfKeywordAdapter = new AutoCompleteAdapter(this, "mf_keywords");
        editView.setAdapter(mfKeywordAdapter);
    }

    private void updateSearchOption() {
        int txtId = searchTextContent ? R.drawable.round_feed_24
                : R.drawable.round_feed_blue_24;
        int caseId = searchResSensitive ? R.drawable.round_text_format_blue_24
                : R.drawable.round_text_format_24;
        searchOptionImage.setImageResource(txtId);
        searchOptionCase.setImageResource(caseId);
    }

    // Setup the manifest search button
    private void setupMfSearch() {
        ImageButton btn = findViewById(R.id.btn_search_mf);
        btn.setOnClickListener(this);
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        // Search a keyword in all strings
        if (id == R.id.search_button) {
            EditText keywordEt = findViewById(R.id.keyword_edit);
            String keyword = keywordEt.getText().toString();
            keyword = keyword.trim();
            if (!keyword.equals("")) {
                searchStringByKeyword(keyword);
                strKeywordAdapter.addInputHistory(keyword);
            } else {
                // Reset the string list
                updateStringList();
            }

            // Hide Input Method
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(
                    getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
        // Search keyword in AndroidManifest.xml
        else if (id == R.id.btn_search_mf) {
            EditText et = findViewById(R.id.mf_keyword);
            String keyword = et.getText().toString();
            keyword = keyword.trim();
            if (keyword.equals("")) {
                Toast.makeText(this, R.string.empty_input_tip,
                        Toast.LENGTH_SHORT).show();
            } else {
                mfKeywordAdapter.addInputHistory(keyword);
                ArrayList<Integer> lines = new ArrayList<>();
                ArrayList<String> lineContents = new ArrayList<>();
                searchManifest(keyword, lines, lineContents);
                if (lines.isEmpty()) {
                    Toast.makeText(this, R.string.notfound_in_manifest,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(this, ManifestSearchResultActivity.class);
                    Bundle bundle = new Bundle();
                    String xmlPath = decodeRootPath + "/AndroidManifest.xml";
                    bundle.putString("filePath", xmlPath);
                    bundle.putIntegerArrayList("lineIndexs", lines);
                    bundle.putStringArrayList("lineContents", lineContents);
                    intent.putExtras(bundle);
                    startActivityForResult(intent, RC_SEARCH_MF);
                }
            }
        }

        // Search in resource
        else if (id == R.id.menu_search_res) {
            EditText et = findViewById(R.id.et_res_keyword);
            String keyword = et.getText().toString();
            keyword = keyword.trim();
            if (keyword.equals("")) {
                Toast.makeText(this, R.string.empty_input_tip, Toast.LENGTH_SHORT).show();
            } else if (resListAdapter != null) {
                // Collect all the file names
                ArrayList<String> filenameList = new ArrayList<>();
                ArrayList<FileRecord> records = new ArrayList<>();
                String curFolder = resListAdapter.getData(records);
                if (!"..".equals(records.get(0).fileName)) {
                    filenameList.add(records.get(0).fileName);
                }
                for (int i = 1; i < records.size(); ++i) {
                    filenameList.add(records.get(i).fileName);
                }
                boolean bSearchFilename = !searchTextContent;
                searchInResourceFiles(keyword, curFolder, filenameList, bSearchFilename, searchResSensitive);
            }
        }

        // Image of dex2smali
        else if (id == R.id.imageview_dex2smali) {
            if (!Preferences.isSmaliLicenseShowed()) {
                new SmaliNoticeDialog(this);
            }
            decodeDex(null);
        }

        // Hide the smali decoding result
        else if (id == R.id.down_arrow_container) {
            dexDecodeLayout.setVisibility(View.GONE);
        }

        // Apply a patch
        else if (id == R.id.menu_apply_patch) {
            new PatchDialog(this, this);
        }

        // Auto translate
        else if (id == R.id.translate) {
            startNewTranslation();
        }
    }

    // Do resource search
    // Search in all the files/folder (specified by fileList)
    protected void searchInResourceFiles(String keyword, String directory,
                                         ArrayList<String> filenameList, boolean bSearchFilename,
                                         boolean caseSensitive) {
        if (resKeywordAdapter != null) {
            resKeywordAdapter.addInputHistory(keyword);
        }

        if (bSearchFilename) {
            new SearchFilenameDialog(this, directory,
                    filenameList, keyword, caseSensitive);
        } else {
            new SearchTextDialog(this, directory, filenameList, keyword, caseSensitive);
        }
    }

    // Return line NO and line content
    private void searchManifest(String keyword, List<Integer> lineIndexs,
                                List<String> lineContents) {
        String filePath = decodeRootPath + "/AndroidManifest.xml";
        try {
            FileInputStream fis = new FileInputStream(filePath);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String line = br.readLine();
            int index = 1;
            while (line != null) {
                if (line.contains(keyword)) {
                    lineIndexs.add(index);
                    lineContents.add(line);
                }
                line = br.readLine();
                index += 1;
            }

            br.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void setupClickListener() {
        saveBtn = findViewById(R.id.btn_build_apk);
        if (BuildConfig.PARSER_ONLY) {
            saveBtn.setVisibility(View.GONE);
        } else {
            saveBtn.setOnClickListener(v -> composeApkFile());
        }

        patchMenu = findViewById(R.id.menu_apply_patch);
        patchMenu.setOnClickListener(this);
    }

    private void collectAndSaveChangedString() {
        // Collect changes
        Map<String, Map<String, String>> changedValues = stringListAdapter.getChangedValues();
        if (changedValues != null && !changedValues.isEmpty()) {
            stringModified = true;
            try {
                saveStringValues(changedValues);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void composeApkFile() {
        if (BuildConfig.LIMIT_NEW_VERSION && MainActivity.Companion.upgradedFromOldVersion(this)) {
            MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(this);
            alert.setTitle(R.string.please_note);
            alert.setMessage(R.string.build_not_support_tip);
            alert.show();
            return;
        }

        collectAndSaveChangedString();

        if (Preferences.isRebuildConfirmEnabled()) {
            Map<String, String> added = resListAdapter.getAddedFiles();
            Map<String, String> replaced = resListAdapter.getReplacedFiles();
            Set<String> deleted = resListAdapter.getDeletedFiles();
            new RebuildConfirmDialog(this, stringModified, manifestModified,
                    added, replaced, deleted).show();
        } else {
            build(true);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Add or import a folder, dialog callback functions

    private void saveStringValues(@NonNull Map<String, Map<String, String>> allChangedValues)
            throws Exception {
        Set<Entry<String, Map<String, String>>> entries = allChangedValues.entrySet();
        for (Entry<String, Map<String, String>> entry : entries) {
//            ResConfigFlags cfgFlags = entry.getKey();
//            String qualifier = cfgFlags.getQualifiers();
            String qualifier = entry.getKey();
            // The value in allStringValues is already changed
            //saveStringResource(qualifier, allStringValues.get(qualifier));
            modifyStringResource(qualifier, entry.getValue());
        }
    }

    public void build(final boolean bSign) {
        resFileModified = false;
        Set<String> modifiedDex = new HashSet<>();

        long resModifyTime = recursiveModifiedTime(new File(decodeRootPath + "/res"));
        Log.d("DEBUG", "resModifyTime=" + resModifyTime);
        long manifestModifyTime = recursiveModifiedTime(new File(decodeRootPath + "/AndroidManifest.xml"));
        Log.d("DEBUG", "manifestTime=" + manifestModifyTime);

        // Collect modified files
        Map<String, String> added = resListAdapter.getAddedFiles();
        Map<String, String> replaced = resListAdapter.getReplacedFiles();
        Set<String> deleted = resListAdapter.getDeletedFiles();

        addedFiles = new HashMap<>();
        replacedFiles = new HashMap<>();
        deletedFiles = new ArrayList<>();
        // Enumerate added files
        for (Entry<String, String> entry : added.entrySet()) {
            String entryName = entry.getKey();
            // Resource file
            if (entryName.startsWith("res/")) {
                if (!resFileModified && !StringHelperKt.findExt(entryName, "jpg|png")) {
                    resFileModified = true;
                }
                addedFiles.put(entry.getKey(), entry.getValue());
            }
            // General other files
            else if (SmaliUtilsKt.dealWithSmaliFile(entryName, modifiedDex) == null) {
                addedFiles.put(entry.getKey(), entry.getValue());
            }
        }

        // Enumerate modified files
        for (Entry<String, String> entry : replaced.entrySet()) {
            String entryName = entry.getKey();
            // Resource file
            if (entryName.startsWith("res/")) {
                if (!resFileModified && !StringHelperKt.findExt(entryName, "jpg|png")) {
                    resFileModified = true;
                }
                replacedFiles.put(entry.getKey(), entry.getValue());
            }
            // General other files
            else if (SmaliUtilsKt.dealWithSmaliFile(entryName, modifiedDex) == null) {
                replacedFiles.put(entry.getKey(), entry.getValue());
            }
        }

        // Enumerate deleted files
        for (String entryName : deleted) {
            // Resource file
            if (entryName.startsWith("res/")) {
                if (!resFileModified && !StringHelperKt.findExt(entryName, "jpg|png")) {
                    resFileModified = true;
                }
                deletedFiles.add(entryName);
            }
            // General other files
            else if (SmaliUtilsKt.dealWithSmaliFile(entryName, modifiedDex) == null) {
                deletedFiles.add(entryName);
            }
        }

        // Collect modified smali folders
        smaliFolders = new ArrayList<>();
        smaliFolders.addAll(modifiedDex);

        launchBuildActivityAndService(bSign);
    }

    @Override
    public void addFolder(String folderName) {
        final String dirPath = resListAdapter.getData(null);
        resListAdapter.addFolder(dirPath, folderName);
    }

    @Override
    public void importFolder(final String folderPath) {
        final String dirPath = resListAdapter.getData(null);
        new ProgressDialog(this, "", "Working…", false,
                new ProgressDialog.ProcessingInterface() {
                    private String errorMessage = null;

                    @Override
                    public void process() {
                        File srcDir = new File(folderPath);
                        String folderName = srcDir.getName();
                        File curDir = new File(dirPath);
                        File targetDir = new File(curDir, folderName);
                        if (targetDir.exists()) {
                            String fmt = getString(R.string.file_already_exist);
                            errorMessage = String.format(fmt, folderName);
                        } else {
                            targetDir.mkdir();
                            try {
                                copyFile(targetDir, srcDir);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void afterProcess() {
                        if (errorMessage != null) {
                            Toast.makeText(ApkInfoActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        } else {
                            resListAdapter.refresh();
                        }
                    }
                }, -1).show();
    }

    // First check if the build is still ongoing
    private void launchBuildActivityAndService(boolean bSign) {
        // If the service is running, then check the build still ongoing or not
        if (ServiceUtil.isMyServiceRunning(this, ApkComposeService.class)) {
            Intent intent = new Intent(this, ApkComposeService.class);
            bindService(intent, new MyServiceConnection(bSign), Context.BIND_AUTO_CREATE);
            return;
        }

        launchWithoutCheck(bSign);
    }

    private void launchWithoutCheck(boolean bSign) {
        String outputDir = ScopedStorage.getApkEditorDir().getPath();
        final String outputApkRule = Preferences.getOutputApkName();
        String filename;
        switch (outputApkRule) {
            case "0":
                filename = apkInfo.pkgName + "_signed";
                break;
            case "2":
                filename = apkInfo.label + "_signed";
                break;
            default:
                filename = "gen_signed";
                break;
        }
        filename = reviseFileName(filename);

        String targetApkPath = createOutputPath(apkPath, outputDir, filename);

        Intent intent = new Intent(this, ApkComposeService.class);
        ActivityHelper.attachParam(intent, "decodeRootPath", decodeRootPath);
        // For full decoding, do NOT pass apkPath, so the builder will know that situation
        if (!isFullDecoding) {
            ActivityHelper.attachParam(intent, "srcApkPath", apkPath);
        }
        ActivityHelper.attachParam(intent, "targetApkPath", targetApkPath);
        ActivityHelper.attachParam(intent, "stringModified", stringModified ? "true" : "false");
        ActivityHelper.attachParam(intent, "manifestModified", manifestModified ? "true" : "false");
        ActivityHelper.attachParam(intent, "resFileModified", resFileModified ? "true" : "false");
        ActivityHelper.attachParam(intent, "modifiedSmaliFolders", smaliFolders);
        ActivityHelper.attachParam(intent, "addedFiles", addedFiles);
        ActivityHelper.attachParam(intent, "deletedFiles", deletedFiles);
        ActivityHelper.attachParam(intent, "replacedFiles", replacedFiles);
        ActivityHelper.attachBoolParam(intent, "signAPK", bSign);

        // It is too big to pass it to another activity
        // So we save it to file
        String mapFileName = serialize2File(mFileEntry2ZipEntry);
        ActivityHelper.attachParam(intent, "fileEntry2ZipEntry", mapFileName);

        startService(intent);

        // Start activity (note: this is activity, not service)
        Intent it = new Intent(this, ApkComposeActivity.class);
        startActivityForResult(it, RC_COMPOSE);
    }

    // Write the map structure to file
    // Return the file name
    @Nullable
    private String serialize2File(Map<String, String> fileEntry2ZipEntry2) {
        try {
            String filepath = ScopedStorage.getTmpDir() + File.separator + getRandomString(8);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filepath));
            final Set<Entry<String, String>> entries = fileEntry2ZipEntry2.entrySet();
            for (Entry<String, String> entry : entries) {
                bos.write(entry.getKey().getBytes());
                bos.write('\n');
                bos.write(entry.getValue().getBytes());
                bos.write('\n');
            }
            bos.close();
            return filepath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Update the view in the center of the screen
    private void updateCenterView() {
        loadingLayout.setVisibility(View.INVISIBLE);
        manifestLayout.setVisibility(View.INVISIBLE);
        stringLayout.setVisibility(View.INVISIBLE);
        resourceLayout.setVisibility(View.INVISIBLE);
        //FIXME

        switch (curSelectedRadio) {
            case 0:
                if (stringParsed) {
                    stringLayout.setVisibility(View.VISIBLE);
                } else {
                    loadingLayout.setVisibility(View.VISIBLE);
                }
                break;
            case 1:
                if (resourceParsed) {
                    resourceLayout.setVisibility(View.VISIBLE);
                } else {
                    loadingLayout.setVisibility(View.VISIBLE);
                }
                break;
            case 2:
                if (resourceParsed) {
                    manifestLayout.setVisibility(View.VISIBLE);
                } else {
                    loadingLayout.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    protected void manifestRadioClicked() {
        if (curSelectedRadio != 2) {
            curSelectedRadio = 2;
            updateCenterView();
        }
    }

    protected void resRadioClicked() {
        // Already in current page
        if (curSelectedRadio == 1) {
            return;
        }

        curSelectedRadio = 1;
        updateCenterView();

        if (!dexDecoded) {
            int showTimes = Preferences.getHideSmaliMsgShown();
            if (showTimes < 1) {
                if (Preferences.isDex2smaliEnabled()) {
                    Toast.makeText(this, R.string.hide_smali_tip, Toast.LENGTH_LONG).show();
                    Preferences.setHideSmaliMsgShown(showTimes + 1);
                }
            }
        }
    }

    protected void stringRadioClicked() {
        if (curSelectedRadio != 0) {
            curSelectedRadio = 0;
            updateCenterView();
        }
    }

    // Called when finished resource table decoding
    @Override
    public void resTableDecoded(boolean ret) {
        if (ret) {
            bStringPrepared = prepareStringList();
            if (bStringPrepared) {
                runOnUiThread(() -> {
                    showStringList();
                    setupClickListener();
                });
            }
        }
    }

    @Override
    public void resourceDecoded(final Map<String, String> fileEntry2ZipEntry) {
        mFileEntry2ZipEntry = fileEntry2ZipEntry;

        // Save the big structure to file
        // So that we do not need to save it every time in onSaveInstanceState
        new Thread(() -> {
            try {
                String workingPath = ScopedStorage.getTmpDir().getPath();
                String path = workingPath + File.separator + "allStringValues";
                writeObjectToFile(path, allStringValues);
                path = workingPath + File.separator + "fileEntry2ZipEntry";
                writeObjectToFile(path, fileEntry2ZipEntry);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // To check whether need to re-show the string list
        final boolean stringShowNeeded = !bStringPrepared;
        if (!bStringPrepared) {
            bStringPrepared = prepareStringList();
        }

        runOnUiThread(() -> {
            if (stringShowNeeded) {
                showStringList();
                setupClickListener();
            }
            showDecodedFileList();
            patchMenu.setVisibility(View.VISIBLE);
            if (!BuildConfig.PARSER_ONLY) {
                saveBtn.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void decodeDex(IGeneralCallback dexDecodedCallback) {
        mDexDecodedCallback = dexDecodedCallback;
        new AsyncDecodeTask(apkPath, decodeRootPath, this).execute();
        dexDecoded = true;
    }

    @Override
    public void decodeFailed(final String errMessage) {
        runOnUiThread(() -> Toast.makeText(ApkInfoActivity.this, errMessage,
                Toast.LENGTH_LONG).show());
    }

    // Collect all the string information
    // It may consume a lot of time, as it may need to decode android system
    // package
    private boolean prepareStringList() {
        allStringValues = new HashMap<>();
        // Set<ResConfigFlags> allConfigs = new HashSet<ResConfigFlags>();

        ResPackage pkg = parseThread.getApkPackage();
        if (pkg == null) {
            return false;
        }
        List<ResResSpec> specList = pkg.listResSpecs();
        for (ResResSpec spec : specList) {

            // String type
            if ("string".equals(spec.getType().getName())) {
                String name = spec.getName();
                String value;

                Map<ResConfigFlags, ResResource> resources = spec.getAllResources();
                for (Entry<ResConfigFlags, ResResource> entry : resources.entrySet()) {
                    String styledValue = null;
                    ResConfigFlags configFlag = entry.getKey();
                    ResResource resResource = entry.getValue();
                    ResValue resVal = resResource.getValue();
                    // String like @string/xxx
                    if (resVal instanceof ResReferenceValue) {
                        value = resVal.toString();
                    } else if (resVal instanceof ResScalarValue) {
                        ResScalarValue scalarVal = ((ResScalarValue) resVal);
                        value = scalarVal.getRawValue();
                        if (value == null) {
                            value = resVal.toString();
                        }
                    } else {
                        value = resVal.toString();
                    }
                    StringItem item = new StringItem(name, value, styledValue);
                    String qualifier = configFlag.getQualifiers();
                    ArrayList<StringItem> stringValueList = allStringValues.get(qualifier);
                    if (stringValueList == null) {
                        stringValueList = new ArrayList<>();
                        allStringValues.put(qualifier, stringValueList);
                    }
                    stringValueList.add(item);
                }
            }
        }
        return true;
    }

    private void showStringList() {
        // Set the changed values to make it recover the previous state (for rotation)
        if (stringListAdapter != null && changedStringValues != null) {
            stringListAdapter.setChangedValues(changedStringValues);
        }

        // Setup listener for adding a button
        initAddLanguageBtn();
        initTranslateBtn();

        // Update string list
        // Do not need to update as initSpinner will setup the string list?
        // updateStringList();

        // Spinner
        initSpinner();

        // Searcher
        ImageButton searchBtn = findViewById(R.id.search_button);
        searchBtn.setOnClickListener(this);

        stringParsed = true;

        updateCenterView();
    }

    private void initAddLanguageBtn() {
        ImageButton iv = findViewById(R.id.add_language);
        iv.setOnClickListener(v -> new LanguageSelectDialog(this, this, null, null));
    }

    // Set the click listener for translate button
    private void initTranslateBtn() {
        ImageButton iv = findViewById(R.id.translate);

        if (generalTranslatePluginExist(this)) {
            iv.setOnClickListener(this);
        } else if (proTranslatePluginExist()) {
            iv.setOnClickListener(this);
        } else {
            iv.setOnClickListener(v -> new AboutPluginDialog(ApkInfoActivity.this));
        }
    }

    private boolean proTranslatePluginExist() {
        try {
            getPackageManager().getApplicationInfo("apkeditor.translate", 0);
            return true;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Start a new translation, it may be called by the translation dialog
    // Show the language selection dialog
    public void startNewTranslation() {
        new LanguageSelectDialog(this, ApkInfoActivity.this, PossibleLanguages.languages, PossibleLanguages.codes);
    }

    // Save Translated strings
    public void saveTranslatedLanguage(String qualifier,
                                       List<StringItem> stringValues) throws Exception {
        StringsUtils.saveStringResource(decodeRootPath, qualifier, stringValues);
        stringModified = true;

        // add to the map structure so that the UI can see it
        String targetConfig = null;
        for (Entry<String, ArrayList<StringItem>> entry : allStringValues.entrySet()) {
            String q = entry.getKey();
            //String q = configFlag.getQualifiers();
            if (q.equals(qualifier)) {
                targetConfig = q;
                ArrayList<StringItem> stringList = entry.getValue();
                stringList.clear();
                stringList.addAll(stringValues);
            }
        }

        // The language does not exist at all
        if (targetConfig == null) {
            targetConfig = qualifier;
            ArrayList<StringItem> stringList = new ArrayList<>(stringValues);
            allStringValues.put(targetConfig, stringList);
        }

        resetSpinner(); // Reset the spinner
    }

    // Show the auto translation dialog (When language selection is completed)
    public void translateLanguage(String strQualifier) {
        // List<TranslateItem> translateList = prepareTranslateItems("-zh-rCN");
        List<TranslateItem> translatedList = new ArrayList<>();
        List<TranslateItem> untranslatedList = new ArrayList<>();
        prepareTranslateItems(strQualifier, translatedList, untranslatedList);

        try {
            Intent intent;
            if (generalTranslatePluginExist(this)) {
                intent = new Intent("android.intent.action.VIEW");
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setDataAndType(null,
                        "application/com.mcal.apkeditor-translate");
            } else {
                ComponentName componetName = new ComponentName(
                        "apkeditor.translate",
                        "apkeditor.translate.TranslateActivity");
                intent = new Intent();
                intent.setComponent(componetName);
            }

            Bundle bundle = new Bundle();
            {
                String translatedFile = ScopedStorage.getTmpDir() + File.separator + "translated";
                writeObjectToFile(translatedFile, translatedList);
                bundle.putString("translatedList_file", translatedFile);
            }
            {
                String untranslatedFile = ScopedStorage.getTmpDir() + File.separator + "untranslatedList";
                writeObjectToFile(untranslatedFile, untranslatedList);
                bundle.putString("untranslatedList_file", untranslatedFile);
            }
            bundle.putString("targetLanguageCode", strQualifier);
            intent.putExtras(bundle);

            startActivityForResult(intent, RC_TRANSLATE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void prepareTranslateItems(String qualifier,
                                       List<TranslateItem> translatedList,
                                       List<TranslateItem> untranslatedList) {

        // We always assume translate from the default string
        ArrayList<StringItem> defaultStrings = null;
        ArrayList<StringItem> translatedStrings = null;
        for (Entry<String, ArrayList<StringItem>> entry : allStringValues.entrySet()) {
//            ResConfigFlags configFlag = entry.getKey();
//            String q = configFlag.getQualifiers();
            String q = entry.getKey();
            if (q.equals(qualifier)) {
                translatedStrings = entry.getValue();
            } else if ("".equals(q)) {
                defaultStrings = entry.getValue();
            }
        }

        // Error, cannot find the source string to translate
        if (defaultStrings == null) {
            if (translatedStrings != null) {
                for (StringItem kv : translatedStrings) {
                    TranslateItem item = new TranslateItem(kv.name, "", kv.value);
                    translatedList.add(item);
                }
            }
            return;
        }

        // Arrange into hash maps
        Map<String, String> translated = new HashMap<>();
        Map<String, String> defaultStrs = new HashMap<>();
        if (translatedStrings != null) {
            for (StringItem p : translatedStrings) {
                translated.put(p.name, p.value);
            }
        }
        for (StringItem p : defaultStrings) {
            defaultStrs.put(p.name, p.value);
        }

        // Add the translated item
        if (translatedStrings != null) {
            for (StringItem p : translatedStrings) {
                String name = p.name;
                String originValue = defaultStrs.get(name);
                String translatedVal = p.value;
                TranslateItem item = new TranslateItem(name, originValue,
                        translatedVal);
                translatedList.add(item);
            }
        }

        // Add the un-translated item
        for (StringItem p : defaultStrings) {
            String name = p.name;
            String translatedVal = translated.get(name);
            if (translatedVal == null) {
                String originValue = p.value;
                TranslateItem item = new TranslateItem(name, originValue, null);
                untranslatedList.add(item);
            }
        }
    }

    // Add string values for a language
    public String addLanguageRetError(@NonNull String qualifier) {
        Resources res = getResources();
        if (qualifier.length() < 3) {
            return res.getString(R.string.invalid_lang_code);
        }

        if (!resourceParsed) {
            return res.getString(R.string.wait_for_decoding);
        }

        // Collect which strings are already translated, which are not
        List<TranslateItem> translatedItems = new ArrayList<>();
        List<TranslateItem> untranslatedItems = new ArrayList<>();
        prepareTranslateItems(qualifier, translatedItems, untranslatedItems);

        // All strings are already in that language
        if (untranslatedItems.isEmpty()) {
            return res.getString(R.string.lang_exist);
        }

        // Put the new value to the map structure
        boolean bExisting = false; // This language previously exist or not
        String addedConfigFlag = null;
        ArrayList<StringItem> untranslatedList = new ArrayList<>();
        for (TranslateItem item : untranslatedItems) {
            untranslatedList.add(new StringItem(item.name, item.originValue));
        }
        // Find the existing config flags
        for (Entry<String, ArrayList<StringItem>> entry : allStringValues.entrySet()) {
//            ResConfigFlags configFlag = entry.getKey();
//            String q = configFlag.getQualifiers();
            String q = entry.getKey();
            if (q.equals(qualifier)) {
                addedConfigFlag = q;
                bExisting = true;
                entry.getValue().addAll(untranslatedList);
                break;
            }
        }
        // The config flags does not exist
        if (addedConfigFlag == null) {
//            addedConfigFlag = createConfigFlags(qualifier);
            addedConfigFlag = qualifier;
            allStringValues.put(addedConfigFlag, untranslatedList);
        }

        // Save added language to resource file
        try {
            if (!bExisting) { // Just need to save the untranslated list
                StringsUtils.saveStringResource(decodeRootPath, qualifier, untranslatedList);
            } else {
                // Need to save both translated and untranslated
                List<StringItem> valueList = new ArrayList<>();
                for (TranslateItem item : translatedItems) {
                    valueList.add(new StringItem(item.name, item.translatedValue));
                }
                valueList.addAll(untranslatedList);
                StringsUtils.saveStringResource(decodeRootPath, qualifier, valueList);
            }
            stringModified = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Update the display
        curConfig = addedConfigFlag;
        // Log.d("DEBUG", "curConfig.qualifier=" + curConfig.getQualifiers());
        resetSpinner(); // Reset the spinner
        // updateStringList(); // Not needed as it will be updated when reset
        // spinner

        return null;
    }

    // Modify the string resource
    // modifiedValues only contain modified values
    private void modifyStringResource(String qualifier,
                                      Map<String, String> modifiedValues) throws Exception {
        String fileName = "strings.xml";

        String dirPath = decodeRootPath + "/res/values" + qualifier;
        File dirFile = new File(dirPath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }

        String filePath = dirPath + "/" + fileName;
        List<String> lines = new TextFileReader(filePath).getLines();
        List<String> newLines = new ArrayList<>();

        // lastStringLine is used to record the position where the new added string should append to
        int lastStringLine = -1;
        final String targetStr = "<string name=\"";
        Set<String> savedStrings = new HashSet<>();
        for (int i = 0; i < lines.size(); ++i) { // Check line by line
            String line = lines.get(i);
            int pos = line.indexOf(targetStr);
            if (pos == -1) {
                newLines.add(line);
                continue;
            }
            int start = pos + targetStr.length();
            int end = line.indexOf('\"', start);
            if (end == -1) {
                newLines.add(line);
                continue;
            }
            String name = line.substring(start, end);
            String newValue = modifiedValues.get(name);
            if (newValue != null) {
                newLines.add(StringItem.toString(name, newValue, null));
                savedStrings.add(name);
                // The string occupies several lines
                if (!line.contains("</string>")) {
                    while (++i < lines.size()) {
                        line = lines.get(i);
                        if (line.contains("</string>")) {
                            break;
                        }
                    }
                }
            } else {
                newLines.add(line);
            }
            lastStringLine = newLines.size() - 1;
        }

        // Add the unsaved string
        for (Map.Entry<String, String> entry : modifiedValues.entrySet()) {
            // The modified value is not saved, the add it
            String name = entry.getKey();
            if (!savedStrings.contains(name)) {
                String newValue = entry.getValue();
                lines.add(++lastStringLine, StringItem.toString(name, newValue, null));
            }
        }

        // Write back to file
        writeToFile(filePath, newLines);
    }

    @SuppressLint("DefaultLocale")
    protected void searchStringByKeyword(@NonNull String keyword) {
        String lcKeyword = keyword.toLowerCase();
        ArrayList<StringItem> values = allStringValues.get(curConfig);
        if (values != null) {
            ArrayList<StringItem> selected = new ArrayList<>();
            for (StringItem pair : values) {
                if (pair.value.toLowerCase().contains(lcKeyword)) {
                    selected.add(pair);
                }
            }
            updateStringList(selected);
        }
    }

    private void updateStringList() {
        if (curConfig == null) {
            curConfig = getBestConfig(allStringValues.keySet());
        }
        ArrayList<StringItem> values = allStringValues.get(curConfig);
        updateStringList(values);
    }

    private void updateStringList(ArrayList<StringItem> values) {
        // Log.d("DEBUG", "updateStringList called!");
        stringListAdapter.updateData(curConfig, values);
    }

    private void initSpinner() {
        // Should not appear, but ...
        if (allStringValues == null) {
            return;
        }
        if (curConfig == null) {
            curConfig = getBestConfig(allStringValues.keySet());
        }

        langConfigList = new ArrayList<>();
        String[] languages = new String[allStringValues.size()];
        List<String> langList = new ArrayList<>();
        Map<String, String> _m = new HashMap<>(); // Language -> qualifier
        for (String qualifier : allStringValues.keySet()) {
            String strLang = LanguageMapping.getLanguage(qualifier);
            langList.add(strLang);
            _m.put(strLang, qualifier);
            // Log.d("DEBUG", "qualifier=" + qualifier + ", " + curConfig);
        }

        // Sort the language
        Collections.sort(langList);
        int selected = 0;
        int index = 0;
        for (String strLang : langList) {
            String configFlag = _m.get(strLang);
            languages[index] = strLang;
            langConfigList.add(configFlag);
            if (configFlag.equals(curConfig)) {
                selected = index;
            }
            index++;
        }

        // Initialize spinner by setting adapter
        Spinner spinner = findViewById(R.id.language_spinner);
        if (spinner == null) {
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Event listener
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int position, long arg3) {
                curConfig = langConfigList.get(position);
                updateStringList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        // Log.d("DEBUG", "spinner, selected=" + selected);
        spinner.setSelection(selected);
    }

    private void resetSpinner() {
        initSpinner();
    }

    // Not only the resource files, but may also include smali files
    private void showDecodedFileList() {
        String curDir;
        if (resCurrentDir != null && resCurrentDir.startsWith(decodeRootPath)) {
            curDir = resCurrentDir;
        } else {
            // Now always show the root path
            //curDir = BuildConfig.IS_PRO ? decodeRootPath : decodeRootPath + "/res";
            curDir = decodeRootPath;
        }
        // For full decoding, do not need apk path (otherwise may cause 2 assets directory)
        String path = (isFullDecoding ? null : apkPath);
        resListAdapter = new ResListAdapter(this, path, curDir,
                decodeRootPath, mFileEntry2ZipEntry, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                // Do not filter AndroidManifest.xml any more
                return true;
//                File f = new File(dir, filename);
//                if (f.isDirectory()) {
//                    return true;
//                } else {
//                    return !(decodeRootPath.equals(dir.getAbsolutePath())
//                            && filename.equals("AndroidManifest.xml"));
//                }
            }
        }, this);
        resListAdapter.setModification(res_addedFiles,
                res_deletedFiles, res_replacedFiles);
        resourceList.setAdapter(resListAdapter);
        resourceList.setOnItemClickListener(this);
        resourceList.setOnItemLongClickListener(this);

        manifestListAdapter = new ManifestListAdapter(this, decodeRootPath + "/AndroidManifest.xml", this);
        manifestRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        manifestRecyclerView.setAdapter(manifestListAdapter);

        // Recover the navigation bar
        if (navigationMgr != null && resCurrentDir != null) {
            navigationMgr.gotoDirectory(resCurrentDir);
        }

        // Set Flag
        resourceParsed = true;

        updateCenterView();
    }

    // Resource directory is changed (notified by navigation manager)
    public void resDirectoryChanged(String newDir, int upLevel) {
        resListAdapter.openDirectory(newDir);
        if (upLevel > 0) {
            try {
                Duo<Integer, Integer> pos = null;
                for (int i = 0; i < upLevel; ++i) {
                    pos = resListPosition.pop();
                }
                if (pos != null) {
                    resourceList.setSelectionFromTop(pos.m1, pos.m2);
                }
            } catch (Exception ignored) {
            }
        }
    }

    // The selection mode of the resource list changed
    @Override
    public void selectionChanged(@NonNull Set<Integer> selected) {
        boolean selectionMode = !selected.isEmpty();
        if (selectionMode) {
            resSearchLayout.setVisibility(View.GONE);
            resNaviHeader.setVisibility(View.GONE);
            resMenuLayout.setVisibility(View.VISIBLE);
            resSelectHeader.setVisibility(View.VISIBLE);
            String text = String.format(getString(R.string.num_items_selected),
                    selected.size());
            resSelectTip.setText(text);
        } else {
            resMenuLayout.setVisibility(View.GONE);
            resSelectHeader.setVisibility(View.GONE);
            resSearchLayout.setVisibility(View.VISIBLE);
            resNaviHeader.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        List<FileRecord> fileList = new ArrayList<>();
        String oldDir = resListAdapter.getData(fileList);
        FileRecord rec = fileList.get(position);
        if (rec == null) {
            return;
        }

        // In selection mode
        if (!resListAdapter.getCheckedItems().isEmpty()) {
            resListAdapter.reverseCheckStatus(position);
            return;
        }

        if (rec.isDir) {
            boolean isUpToParent = false;
            String targetPath;
            if (rec.fileName.equals("..")) {
                isUpToParent = true;
                int pos = oldDir.lastIndexOf('/');
                targetPath = oldDir.substring(0, pos);
            } else {
                // save index and top position
                int index = resourceList.getFirstVisiblePosition();
                View v = resourceList.getChildAt(0);
                int top = (v == null) ? 0
                        : (v.getTop() - resourceList.getPaddingTop());
                resListPosition.push(new Duo<>(index, top));

                targetPath = oldDir + "/" + rec.fileName;
            }
            resListAdapter.openDirectory(targetPath);
            navigationMgr.gotoDirectory(targetPath);
            if (isUpToParent) { // Recover from the old position
                try {
                    Duo<Integer, Integer> pos = resListPosition.pop();
                    resourceList.setSelectionFromTop(pos.m1, pos.m2);
                } catch (Exception ignored) {
                }
            } else {
                resourceList.setSelectionAfterHeaderView();
            }

            // After open the folder, check if it exist
            // String curFolder = resListAdapter.getData(null);
            // if (new File(curFolder).exists()) {
            // this.resSearchLayout.setVisibility(View.VISIBLE);
            // } else {
            // this.resSearchLayout.setVisibility(View.GONE);
            // }
        }
        // Try to open the file
        else {
            openFile(oldDir, rec.fileName, rec.isInZip);
        }
    }

    // Open files like *.xml, *.txt, etc
    private void openEditableFile(String directory, String fileName,
                                  boolean bInZip, String entryName, String syntaxFileName) {

        String filePath = resListAdapter.getReplacedFilePath(entryName);
        if (filePath != null) {
            // replaced entry
        } else if (bInZip) {
            // Extract to temp path
            filePath = extractFileFromZip(entryName);
            if (filePath == null) {
                String errMeesage = String.format(getString(R.string.cannot_open_xxx), entryName);
                Toast.makeText(this, errMeesage, Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            filePath = directory + "/" + fileName;
        }
        openFileEditor(filePath, syntaxFileName, fileName, entryName);
    }

    public void openFile(@NonNull String directory, String fileName, boolean bInZip) {
        // Get entry name
        String entryName;
        if (directory.equals(decodeRootPath)) {
            entryName = fileName;
        } else {
            entryName = directory.substring(decodeRootPath.length() + 1) + "/"
                    + fileName;
        }

        String syntaxFileName = getEditableSyntax(fileName);
        if (syntaxFileName != null) {
            openEditableFile(directory, fileName, bInZip, entryName, syntaxFileName);
        } else {
            String filePath = resListAdapter.getReplacedFilePath(entryName);
            if (filePath != null) {
                // replaced entry
            } else if (bInZip) {
                // Extract to temp path
                filePath = extractFileFromZip(entryName);
            } else if (resListAdapter.isImageFile(fileName)) {
                // Image file also need to extract
                filePath = extractFileFromZip(entryName);
            } else {
                // Need to copy to sd card?
                filePath = directory + "/" + fileName;
            }

            // Avoid null path, don't know when it is null, but seems it could be happened
            if (filePath != null) {
                filePathForExternal = filePath;
                entryNameForExternal = entryName;
                File f = new File(filePath);
                modifiedTimeBeforeOpen = f.lastModified();
                if (StringHelperKt.findExt(filePath, "jpg|jpeg|png|gif")) {
                    Intent intent = new Intent(this, PhotoViewerActivity.class);
                    ActivityHelper.attachParam(intent, "filePath", filePath);
                    startActivityForResult(intent, RC_OPEN_EXTERNAL);
                } else {
                    OpenFiles.openFile(this, filePath, RC_OPEN_EXTERNAL);
                }
            }
        }
    }

    // Get the syntax file name for the file when editing
    @Nullable
    private String getEditableSyntax(@NonNull String fileName) {
        if (fileName.endsWith(".xml")) {
            return "xml.xml";
        } else if (fileName.endsWith(".smali")) {
            return "smali.xml";
        } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "html.xml";
        } else if (fileName.endsWith(".css")) {
            return "css.xml";
        } else if (fileName.endsWith(".java")) {
            return "java.xml";
        } else if (fileName.endsWith(".json")) {
            return "json.xml";
        } else if (fileName.endsWith(".txt") || fileName.endsWith(".yml")) {
            return "txt.xml";
        } else if (fileName.endsWith(".js")) {
            return "js.xml";
        }
        return null;
    }

    public void replaceFile(String replacedPath, String replacingPath) {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            // Copy files
            in = new FileInputStream(replacingPath);
            out = new FileOutputStream(replacedPath);
            copyFile(in, out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }

    // filePath: file to be edited
    // syntaxFileName: like html.xml, smali.xml
    // displayFileName: name to be shown in title
    // entryName: the editing entry in apk file (here as extraString)
    // Note: entryName can NOT be null
    private void openFileEditor(String filePath, String syntaxFileName,
                                String displayFileName, String entryName) {
        // Open the color editor
        if ("res/values/colors.xml".equals(entryName)) {
            Intent intent = new Intent(this, ColorXmlActivity.class);
            ActivityHelper.attachParam(intent, "filePath", filePath);
            startActivityForResult(intent, RC_COLOR_EDITOR);
            return;
        }

        Intent intent = TextEditor.getSoraEditor(this, filePath, apkPath, 0, null);
        startActivityForResult(intent, RC_FILE_EDITOR);
    }

    // Long click the resource list item
    @Override
    public boolean onItemLongClick(@NonNull AdapterView<?> parent, View arg1,
                                   final int position, long id) {
        // The first item is always the parent folder
        final boolean isFirstItem = (position == 0);

        parent.setOnCreateContextMenuListener(
                (menu, v, menuInfo) -> {
                    // Check the item is directory or not
                    List<FileRecord> records = new ArrayList<>();
                    String curPath = resListAdapter.getData(records);
                    boolean isDir = records.get(position).isDir;

                    // Delete
                    if (!isFirstItem) {
                        MenuItem item1 = menu.add(0, Menu.FIRST, 0,
                                R.string.delete);
                        item1.setOnMenuItemClickListener(
                                item -> {
                                    List<Integer> positions = new ArrayList<>(1);
                                    positions.add(position);
                                    resListAdapter.deleteFile(positions);
                                    resListAdapter.dumpChangedFiles();
                                    return true;
                                });
                    }
                    // Extract (res directory also allow to extract)
                    if (!isFirstItem || curPath.equals(decodeRootPath)) {
                        MenuItem item2 = menu.add(0, Menu.FIRST + 1, 0,
                                R.string.extract);
                        item2.setOnMenuItemClickListener(
                                item -> {
                                    List<Integer> positions = new ArrayList<>(1);
                                    positions.add(position);
                                    extractFileOrDir(positions);
                                    return true;
                                });
                    }
                    // Replace the file/folder
                    if (!isFirstItem || curPath.equals(decodeRootPath)) {
                        MenuItem item3 = menu.add(0, Menu.FIRST + 2, 0,
                                R.string.replace);
                        OnMenuItemClickListener listener;
                        if (isDir) {
                            listener = item -> {
                                replaceFolder(position);
                                resListAdapter.dumpChangedFiles();
                                return true;
                            };
                        } else {
                            listener = item -> {
                                replaceFile(position);
                                resListAdapter.dumpChangedFiles();
                                return true;
                            };
                        }
                        item3.setOnMenuItemClickListener(listener);
                    }

                    // Add a file (always allow to add a file)
                    {
                        MenuItem item4 = menu.add(0, Menu.FIRST + 3, 0,
                                R.string.add_a_file);
                        item4.setOnMenuItemClickListener(
                                item -> {
                                    addFile(position);
                                    // resListAdapter.dumpChangedFiles();
                                    return true;
                                });
                    }

                    // Add a folder (always allow to add a folder)
                    {
                        MenuItem item5 = menu.add(0, Menu.FIRST + 4, 0,
                                R.string.new_folder);
                        item5.setOnMenuItemClickListener(
                                item -> {
                                    createFolder(position);
                                    return true;
                                });
                    }
                });
        return false;
    }

    @NonNull
    private List<FileCopyDialog.CopySource> getCopySources(@NonNull List<Integer> positions) {
        List<FileCopyDialog.CopySource> result = new ArrayList<>();

        List<FileRecord> records = new ArrayList<>();
        String dirPath = resListAdapter.getData(records);

        for (int position : positions) {
            if (position >= records.size()) {
                continue;
            }

            FileRecord fileRec = records.get(position);

            FileCopyDialog.CopySource src = new FileCopyDialog.CopySource();
            if (fileRec.isInZip) {
                src.path = (dirPath + "/" + fileRec.fileName)
                        .substring(decodeRootPath.length() + 1);
            } else {
                src.path = dirPath + "/" + fileRec.fileName;
            }
            src.isInApk = fileRec.isInZip;
            src.isDir = fileRec.isDir;

            result.add(src);
        }
        return result;
    }

    // Extract file/directory to sd card
    protected void extractFileOrDir(List<Integer> positions) {
        List<FileCopyDialog.CopySource> sources = getCopySources(positions);
        extractFileOrDir_internal(sources);
    }

    // Extract file from a path, not a entry
    public void extractFileOrDir(String filepath) {
        List<FileCopyDialog.CopySource> sources = new ArrayList<>();
        FileCopyDialog.CopySource s = new FileCopyDialog.CopySource();
        s.isInApk = false;
        s.isDir = false;
        s.path = filepath;
        sources.add(s);
        extractFileOrDir_internal(sources);
    }

    private void extractFileOrDir_internal(
            final List<FileCopyDialog.CopySource> sources) {
        // Select a target folder to extract
        String dlgTitle = getString(R.string.select_folder);
        IFileSelection callback = new IFileSelection() {
            @Override
            // filePath is the target directory
            // extraStr is the source file/directory
            public void fileSelectedInDialog(
                    String filePath, String extraStr, boolean openFile) {
                FileCopyDialog d = new FileCopyDialog(ApkInfoActivity.this, apkPath, decodeRootPath, mFileEntry2ZipEntry, sources, filePath);
                d.show();
            }

            @Override
            public boolean isInterestedFile(String filename, String extraStr) {
                return true;
            }

            @Nullable
            @Contract(pure = true)
            @Override
            public String getConfirmMessage(String filePath, String extraStr) {
                return null;
            }
        };

        new FileSelectDialog(this, callback, null, null,
                dlgTitle, true, false, false, null);
    }

    // Extract a file from apk to a temporary path
    @Nullable
    private String extractFileFromZip(@NonNull String entryName) {
        // Get file type
        String filename;
        int pos = entryName.lastIndexOf("/");
        if (pos != -1) {
            filename = entryName.substring(pos + 1);
        } else {
            filename = entryName;
        }
        String fileType = "";
        pos = filename.lastIndexOf(".");
        if (pos != -1) {
            fileType = filename.substring(pos);
        }

        try {
            String zipEntry = mFileEntry2ZipEntry.get(entryName);
            String dstPath = ScopedStorage.getTmpDir() + File.separator + TMP_EDITOR_FILE + fileType;
            if (zipEntry != null) {
                entryName = zipEntry;
            }
            ZipHelper.unzipFileTo(apkPath, entryName, dstPath);
            return dstPath;
        } catch (Exception e1) {
            return null;
        }
    }

    // To replace a resource file by showing the a file select dlg
    protected void replaceFile(int position) {
        List<FileRecord> records = new ArrayList<>();
        String dirPath = resListAdapter.getData(records);
        FileRecord rec = records.get(position);
        String filepath = dirPath + "/" + rec.fileName;
        replaceFile(filepath, (SomethingChangedListener) null);
    }

    // replacedPath, also called as decodedPath, will be replaced
    public void replaceFile(@NonNull String replacedPath,
                            final SomethingChangedListener listener) {
        String suffix = null;
        int slashPosition = replacedPath.lastIndexOf('/');
        String fileName = replacedPath.substring(slashPosition + 1);
        int dotPosition = fileName.lastIndexOf('.');
        if (dotPosition != -1) {
            suffix = fileName.substring(dotPosition);
        }
        final String fileTypeStr = suffix;

        new FileSelectDialog(this,
                new IFileSelection() {
                    @Override
                    public void fileSelectedInDialog(
                            String filePath, String extraStr, boolean openFile) {
                        if (openFile) {
                            // Save the param before launch a new activity
                            saveParams(filePath, extraStr, listener);
                            OpenFiles.openFile(
                                    ApkInfoActivity.this, filePath, RC_OPEN_BEFORE_REPLACE);
                        } else {
                            resListAdapter.replaceFile(extraStr, filePath);
                            if (listener != null) {
                                listener.somethingChanged();
                            }
                        }
                    }

                    @Override
                    public boolean isInterestedFile(String filename, String extraStr) {
                        if (fileTypeStr != null) {
                            return filename.endsWith(fileTypeStr);
                        }
                        return true;
                    }

                    @Override
                    public String getConfirmMessage(String filePath, String extraStr) {
                        return null;
                    }
                }, suffix, replacedPath, null, false, false, true, null);
    }

    // Replace a folder
    protected void replaceFolder(int position) {
        String dlgTitle = getString(R.string.select_folder_replace);

        List<FileRecord> records = new ArrayList<>();
        String dirPath = resListAdapter.getData(records);
        FileRecord rec = records.get(position);

        IFileSelection callback = new IFileSelection() {
            @Override
            public void fileSelectedInDialog(String filePath, String decodedPath, boolean openFile) {
                String workingDir = ScopedStorage.getTmpDir().getPath();
                // Selected path contains working dir
                if (workingDir.startsWith(filePath)) {
                    Toast.makeText(ApkInfoActivity.this,
                                    R.string.select_folder_err2, Toast.LENGTH_LONG)
                            .show();
                }
                // Selected path inside working dir
                else if (filePath.startsWith(workingDir)) {
                    Toast.makeText(ApkInfoActivity.this,
                                    R.string.select_folder_err1, Toast.LENGTH_LONG)
                            .show();
                } else {
                    resListAdapter.replaceFolder(decodedPath, filePath);
                }
            }

            @Override
            public boolean isInterestedFile(String filename, String extraStr) {
                return true;
            }

            @NonNull
            @Override
            public String getConfirmMessage(String filePath, @NonNull String extraStr) {
                String replaced = extraStr.substring(decodeRootPath.length() + 1);
                String message = getString(R.string.folder_replace_tip);
                return String.format(message, replaced, filePath);
            }
        };

        new FileSelectDialog(this, callback, null, dirPath + "/" + rec.fileName, dlgTitle, true, true, false, null);
    }

    // To add a file in current directory
    protected void addFile(int position) {
        String dirPath = resListAdapter.getData(null);
        new FileSelectDialog(this,
                new IFileSelection() {
                    @Override
                    public void fileSelectedInDialog(
                            String filePath, String extraStr, boolean openFile) {
                        String name = filePath
                                .substring(filePath.lastIndexOf("/") + 1);
                        resListAdapter.addFile(extraStr + "/" + name, filePath);
                    }

                    @Override
                    public boolean isInterestedFile(String filename, String extraStr) {
                        return true;
                    }

                    @Override
                    public String getConfirmMessage(String filePath,
                                                    String extraStr) {
                        return null;
                    }
                }, null, dirPath, getString(R.string.add_a_file));
    }

    // To create a folder in current directory
    protected void createFolder(int position) {
        boolean showImportFolder = isFullDecoding;
        new AddFolderDialog(this, this, showImportFolder);
    }

    @Override
    public String tryToDeleteSection(LineRecord lineRec) {
        return null;
    }

    @Override
    public void manifestChanged(@NonNull String newContent) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(decodeRootPath + "/AndroidManifest.xml");
            fos.write(newContent.getBytes());
            manifestModified = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // To show the dex decoding progress bar
    @Override
    public void dexDecodingStarted() {
        dex2smaliImage.setVisibility(View.INVISIBLE);
        decodeResultLayout.setVisibility(View.INVISIBLE);
        decodeProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void dexDecodingFinished(boolean ret, String strError,
                                    String strWarning) {
        // Notify the dex decoded watcher
        if (mDexDecodedCallback != null) {
            mDexDecodedCallback.callbackFunc();
            mDexDecodedCallback = null;
        }

        boolean showResult = true;
        if (ret) {
            if (strWarning != null) {
                decodeResultTitle.setText(R.string.succeed_with_warning);
                String content = getString(R.string.warning) + ": " + strWarning;
                decodeResultDetail.setText(content);
                decodeResultDetail.setVisibility(View.VISIBLE);
            } else {
                showResult = false;
                decodeResultTitle.setText(R.string.succeed);
                decodeResultDetail.setVisibility(View.GONE);
            }
        } else {
            decodeResultTitle.setText(R.string.failed);
            decodeResultDetail.setVisibility(View.VISIBLE);
            if (strError != null) {
                decodeResultDetail.setText(strError);
            } else {
                decodeResultDetail.setText(R.string.unknown_error);
            }
        }

        // Switch the layout (decoding panel)
        dex2smaliImage.setVisibility(View.INVISIBLE);
        decodeProgressBar.setVisibility(View.INVISIBLE);
        if (showResult) {
            decodeResultLayout.setVisibility(View.VISIBLE);
        } else {
            dexDecodeLayout.setVisibility(View.GONE);
            Toast.makeText(this, R.string.dex_decode_succeed, Toast.LENGTH_LONG).show();
        }

        // Update the file list if in the root decoded folder
        String curFolder = resListAdapter.getData(null);
        if (curFolder.endsWith("/decoded")) {
            resListAdapter.openDirectory(curFolder);
        }
    }

    @Override
    public boolean onLongClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.imageview_dex2smali) {
            dexDecodeLayout.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    @Override
    public String getDecodeRootPath() {
        return decodeRootPath;
    }

    @Override
    public ResListAdapter getResListAdapter() {
        return resListAdapter;
    }

    @Override
    public boolean isDexDecoded() {
        return dexDecoded;
    }

    @Override
    public String getApkPath() {
        return apkPath;
    }

    @Override
    public ApkInfoParser.AppInfo getApkInfo() {
        return apkInfo;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    // Save params before launching the new Activity for file view/editing
    // In the scenario of file replacing
    public void saveParams(String filePath, String extraStr, SomethingChangedListener listener) {
        savedParam_filePath = filePath;
        savedParam_extraStr = extraStr;
        savedParam_listener = listener;
    }

    class MyServiceConnection implements ServiceConnection {
        private final boolean mSign;

        public MyServiceConnection(boolean sign) {
            mSign = sign;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ApkComposeService.ComposeServiceBinder binder = (ApkComposeService.ComposeServiceBinder) service;
            if (binder.isRunning()) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ApkInfoActivity.this)
                        .setMessage(R.string.build_in_progress_tip)
                        .setTitle(R.string.please_note)
                        .setPositiveButton(android.R.string.ok, null);
                builder.show();
            } else {
                launchWithoutCheck(mSign);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }
}