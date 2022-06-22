package com.mcal.apkeditor.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.ApkListAdapter;
import com.mcal.apkeditor.BuildConfig;
import com.mcal.apkeditor.GlobalConfig;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.dialogs.EditModeView;
import com.mcal.apkeditor.se.SimpleEditActivity;
import com.mcal.common.activities.CustomizedLangActivity;
import com.mcal.common.data.Preferences;
import com.mcal.common.utils.ActivityUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ApkSearchActivity extends CustomizedLangActivity implements OnItemClickListener,
        EditModeView.IEditModeSelected {

    private final List<String> apkFileList = new ArrayList<>();
    private String keyword;
    private String searchPath;
    private TextView titleTV;
    private View searchingLayout;
    private ApkListAdapter apkListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_apksearch);
        initFullScreen();

        Intent intent = getIntent();
        this.keyword = ActivityUtils.getParam(intent, "Keyword");
        this.searchPath = ActivityUtils.getParam(intent, "Path");

        initView();

        // Start the searching thread
        new ApkSearchThread().start();
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

    private void initView() {
        this.titleTV = (TextView) this.findViewById(R.id.title);
        this.searchingLayout = this.findViewById(R.id.searching_layout);
        ListView apkListView = (ListView) this.findViewById(R.id.listview_apkfiles);
        this.apkListAdapter = new ApkListAdapter(this);
        apkListView.setAdapter(apkListAdapter);
        apkListView.setOnItemClickListener(this);
        setTitleText(0);
    }

    // To notify the an apk file is found
    public void foundApkFile(final String apkPath) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                apkFileList.add(apkPath);
                setTitleText(apkFileList.size());
                apkListAdapter.addApkFile(apkPath);
            }
        });
    }

    private void setTitleText(int apkNum) {
        String title = String.format(
                getString(R.string.str_files_found), apkNum, keyword);
        if ("".equals(keyword)) { // Not show the last "- ''"
            title = title.substring(0, title.length() - 4);
        }
        titleTV.setText(title);
    }

    // To notify the searching is done
    public void apkSearchDone() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                searchingLayout.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        if (position < this.apkFileList.size()) {
            String filePath = this.apkFileList.get(position);
            if (BuildConfig.PARSER_ONLY) {
                UserAppActivity.startFullEditActivity(this, filePath);
            } else if (BuildConfig.LIMIT_NEW_VERSION && MainActivity.upgradedFromOldVersion(this)) {
                startFullEditActivity(filePath);
            } else {
                new EditModeView(this, this, filePath, null).showFileEditDialog(arg1);
            }
        }
    }

    private void startFullEditActivity(String filePath) {
        UserAppActivity.startFullEditActivity(this, filePath);
    }

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

    class ApkSearchThread extends Thread {
        String lcKey;

        ApkSearchThread() {
            lcKey = keyword.toLowerCase();
        }

        @Override
        public void run() {
            searchInFolder(new File(searchPath));
            apkSearchDone();
        }

        private void searchInFolder(@NonNull File curDir) {
            File[] files = curDir.listFiles();
            if (files != null) {
                List<File> dirList = new ArrayList<>();
                for (File f : files) {
                    if (f.isFile()) {
                        String fileName = f.getName();
                        if (fileName.endsWith(".apk")
                                && fileName.toLowerCase().contains(lcKey)) {
                            foundApkFile(f.getAbsolutePath());
                        }
                    } else { // Directory
                        dirList.add(f);
                    }
                }
                // Search in all sub directories
                for (File dir : dirList) {
                    searchInFolder(dir);
                }
            }
        }
    }
}
