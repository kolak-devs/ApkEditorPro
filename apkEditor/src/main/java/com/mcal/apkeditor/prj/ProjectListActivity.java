package com.mcal.apkeditor.prj;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.mcal.apkeditor.ApkInfoActivity;
import com.mcal.apkeditor.BuildConfig;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.dialogs.ProcessingDialog;
import com.mcal.apkeditor.util.FileUtils;
import com.mcal.common.utils.ApkInfoParser;
import com.mcal.common.data.Preferences;
import com.mcal.common.utils.ScopedStorage;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.types.ProjectInfo;

public class ProjectListActivity extends AppCompatActivity implements View.OnClickListener {
    private final MyHandler handler = new MyHandler(this);
    private ProjectListAdapter adapter;
    private String projectFolder; // like "/sdcard/ApkEditor/.projects/"
    private List<ProjectListAdapter.ItemInfo> projectItems;
    private IconParseThread thread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        if (Preferences.getFullScreen()) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.activity_projectlist);

        setupToolbar(R.string.projects);
        initUI();
    }

    @SuppressWarnings("ConstantConditions")
    private void setupToolbar(int title) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public void onClick(@NonNull View view) {
        int id = view.getId();

        if (id == R.id.menu_delete) {
            int i = (Integer) view.getTag();
            if (i < projectItems.size()) {
                ProjectListAdapter.ItemInfo item = projectItems.get(i);
                removeProject(item);
            }
        }
    }

    private void removeProject(ProjectListAdapter.ItemInfo item) {
        new ProcessingDialog(this, new ProjectRemover(this, item), -1).show();
    }

    void updateProjectList() {
        projectItems = listProjects(projectFolder);
        adapter.updateData(projectItems);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        if (thread != null && thread.isAlive()) {
            thread.stopParse();
        }
        super.onDestroy();
    }

    private void initUI() {
        ListView projectList = findViewById(R.id.project_list);

        try {
            // For APK Parser, no extra project information
            if (BuildConfig.PARSER_ONLY) {
                this.projectFolder = ScopedStorage.getStorageDirectory() + "/ApkParser";
            } else {
                this.projectFolder = FileUtils.makeDir(this, ".projects");
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        // Set list adapter
        projectItems = listProjects(projectFolder);
        adapter = new ProjectListAdapter(this, projectItems);
        projectList.setAdapter(adapter);
        projectList.setOnItemClickListener(adapter);
        thread = new IconParseThread();
        thread.start();
    }

    protected List<ProjectListAdapter.ItemInfo> listProjects(String projectFolder) {
        File prjDir = new File(projectFolder);

        List<ProjectListAdapter.ItemInfo> items = new ArrayList<>();

        do {
            File[] files = prjDir.listFiles();
            if (files == null) {
                break;
            }

            for (File f : files) {
                if (f.isFile()) {
                    continue;
                }
                File prj = findProjectFile(f.listFiles());
                if (prj == null) {
                    continue;
                }

                ProjectInfo info = ApkInfoActivity.loadProject(f.getPath());
                if (info == null) {
                    continue;
                }

                items.add(new ProjectListAdapter.ItemInfo(f.getName(),
                        info.apkPath, info.decodeRootPath, prj.lastModified()));
            }
        } while (false);

        if (!items.isEmpty()) {
            Comparator<ProjectListAdapter.ItemInfo> comparator =
                    new Comparator<ProjectListAdapter.ItemInfo>() {
                        @Contract(pure = true)
                        @Override
                        public int compare(@NonNull ProjectListAdapter.ItemInfo arg0,
                                           @NonNull ProjectListAdapter.ItemInfo arg1) {
                            return arg0.lastModified < arg1.lastModified ? 1 : -1;
                        }
                    };
            Collections.sort(items, comparator);
        }

        return items;
    }

    // Look for ae.prj
    private File findProjectFile(File[] files) {
        if (files == null) {
            return null;
        }
        for (File f : files) {
            if (f.isFile() && f.getName().equals("ae.prj")) {
                return f;
            }
        }
        return null;
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    static class MyHandler extends Handler {
        private final Map<String, Drawable> icons = new HashMap<>();
        private final WeakReference<ProjectListActivity> actRef;

        MyHandler(ProjectListActivity activity) {
            actRef = new WeakReference<>(activity);
        }

        void setIcon(String apkPath, Drawable drawable) {
            synchronized (icons) {
                icons.put(apkPath, drawable);
            }
            sendEmptyMessage(0);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 0:
                    synchronized (icons) {
                        actRef.get().adapter.setProjectIcon(icons);
                    }
                    actRef.get().adapter.notifyDataSetChanged();
                    break;
            }
        }
    }

    class IconParseThread extends Thread {
        private boolean stopFlag = false;

        void stopParse() {
            stopFlag = true;
        }

        @Override
        public void run() {
            ApkInfoParser parser = new ApkInfoParser();
            int index = 0;
            while (!stopFlag && index < projectItems.size()) {
                ProjectListAdapter.ItemInfo item = projectItems.get(index);
                try {
                    ApkInfoParser.AppInfo info =
                            parser.parse(ProjectListActivity.this, item.apkPath);
                    handler.setIcon(item.apkPath, info.icon);
                } catch (Exception ignored) {
                }
                index += 1;
            }
        }
    }
}