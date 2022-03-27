package com.mcal.apkeditor.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.ContentLoadingProgressBar;

import com.mcal.apkeditor.AppInfo;
import com.mcal.apkeditor.AppListAdapter;
import com.mcal.apkeditor.BuildConfig;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.dialogs.EditModeView;
import com.mcal.apkeditor.dialogs.ProcessingDialog;
import com.mcal.apkeditor.se.SimpleEditActivity;
import com.mcal.apkeditor.utils.FileUtils;
import com.mcal.appdm.PrefOverallActivity;
import com.mcal.common.utils.ActivityUtils;
import com.mcal.common.activities.CustomizedLangActivity;
import com.mcal.common.utils.IOUtils;
import com.mcal.common.data.Preferences;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UserAppActivity extends CustomizedLangActivity implements OnItemClickListener,
        OnClickListener, EditModeView.IEditModeSelected {

    private final MyHandler handler = new MyHandler(this);
    private final List<AppInfo> allApps = new ArrayList<>();
    private final List<AppInfo> displayApps = new ArrayList<>();
    private final int APPINFO_ID = Menu.FIRST + 1;
    private final int BACKUP_ID = Menu.FIRST + 2;
    private final int LAUNCH_ID = Menu.FIRST + 3;
    // appTypeIndex = 0, show user apps; appTypeIndex = 1, show system apps
    private int appTypeIndex = 0;
    private int appOrderIndex = 0; // 0 means by name
    private ListView appListView;
    private AppCompatEditText keywordEdit;
    private AppCompatImageButton searchBtn;
    private ContentLoadingProgressBar progressBar;
    private MenuItem userApps;
    private MenuItem systemApps;
    private MenuItem sortByApp;
    private MenuItem sortByInstallTime;

    public static boolean startFullEditActivity(final Activity activity, final String filePath) {
        //String mode = SettingsFragment.getDecodeMode(activity);

        // to be dynamically selected
        /*if ("2".equals(mode)) {
            Dialogs.showDecodeModeSheet(activity, filePath, false);
            return false;
        } else {
            startFullEditActivity(activity, filePath, mode);
            return true;
        }*/
        startFullEditActivity(activity, filePath, "0");
        return true;
    }

    // mode = "0" means full editing
    public static void startFullEditActivity(Context ctx, String filePath, String mode) {
        Intent intent = new Intent(ctx, ApkInfoExActivity.class);
        ActivityUtils.attachParam(intent, "apkPath", filePath);
        boolean fullDecoding = "0".equals(mode);
        ActivityUtils.attachBoolParam(intent, "isFullDecoding", fullDecoding);
        ctx.startActivity(intent);
    }

    public static boolean openApp(@NonNull Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        try {
            Intent i = manager.getLaunchIntentForPackage(packageName);
            if (i == null) {
                return false;
                //throw new ActivityNotFoundException();
            }
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(i);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        if (Preferences.getFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.activity_applist);

        setupToolbar(getString(R.string.select_apk_from_app));

        // Get default app order
        String strOrder = Preferences.getListOrder();
        this.appOrderIndex = getOrderIndex(strOrder);

        initUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_applist, menu);

        userApps = menu.findItem(R.id.user_apps);
        systemApps = menu.findItem(R.id.system_apps);
        sortByApp = menu.findItem(R.id.sort_by_app);
        sortByInstallTime = menu.findItem(R.id.sort_by_install_time);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.user_apps) {
            reScanAppList(0, appOrderIndex);
            systemApps.setChecked(false);
            userApps.setChecked(true);
        } else if (id == R.id.system_apps) {
            reScanAppList(1, appOrderIndex);
            userApps.setChecked(false);
            systemApps.setChecked(true);
        } else if (item.getItemId() == R.id.sort_by_app) {
            reScanAppList(appTypeIndex, 0);
            sortByInstallTime.setChecked(false);
            sortByApp.setChecked(true);
        } else if (item.getItemId() == R.id.sort_by_install_time) {
            reScanAppList(appTypeIndex, 1);
            sortByApp.setChecked(false);
            sortByInstallTime.setChecked(true);
        } else if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        reScanAppList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void showAppList(List<AppInfo> apps) {
        this.progressBar.setVisibility(View.GONE);
        this.appListView.setVisibility(View.VISIBLE);

        AppListAdapter adapter = (AppListAdapter) appListView.getAdapter();
        String[] orders = getResources().getStringArray(R.array.order_value);
        String strOrder = (appOrderIndex < orders.length) ? orders[appOrderIndex] : "";
        adapter.setAppList(apps, strOrder);
        adapter.notifyDataSetChanged();

        synchronized (this.displayApps) {
            this.displayApps.clear();
            this.displayApps.addAll(apps);
        }

        // Enable the search
        this.keywordEdit.setEnabled(true);
        this.searchBtn.setEnabled(true);
    }

    // Get app order index from the string
    private int getOrderIndex(String strOrder) {
        String[] orders = getResources().getStringArray(R.array.order_value);
        for (int i = 0; i < orders.length; ++i) {
            if (strOrder.equals(orders[i])) {
                return i;
            }
        }
        return 0;
    }

    protected void initAppList() {
        AppListAdapter adapter = new AppListAdapter(this);
        adapter.setAppList(displayApps, Preferences.getListOrder());
        appListView.setAdapter(adapter);
        appListView.setOnItemClickListener(this);
        this.registerForContextMenu(appListView);
    }

    private void initUI() {
        progressBar = this.findViewById(R.id.progress_bar);

        appListView = this.findViewById(R.id.application_list);
        initAppList();

        this.keywordEdit = this.findViewById(R.id.et_keyword);
        this.searchBtn = this.findViewById(R.id.btn_search);
        searchBtn.setOnClickListener(this);
    }

    @SuppressWarnings("ConstantConditions")
    private void setupToolbar(String title) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void reScanAppList(int typeIndex, int orderIndex) {
        this.appTypeIndex = typeIndex;
        this.appOrderIndex = orderIndex;
        reScanAppList();
    }

    private void reScanAppList() {
        new Thread() {
            @Override
            public void run() {
                // To get the data
                List<AppInfo> appList = new ArrayList<>();
                getAppList(appList);

                // To refresh UI
                handler.setAppList(appList);
                handler.sendEmptyMessage(0);
            }
        }.start();
    }

    protected void getAppList(List<AppInfo> appList) {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> appInfoList = pm.getInstalledApplications(0);

        // Show user apps
        if (appTypeIndex == 0) {
            for (ApplicationInfo ai : appInfoList) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    // When do some recording, to enable following line
                    //if (ai.enabled)
                    appList.add(AppInfo.create(pm, ai));
                }
            }
        }
        // Show system apps
        else {
            for (ApplicationInfo ai : appInfoList) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    appList.add(AppInfo.create(pm, ai));
                }
            }
        }
    }

    private AppInfo getAppInfo(int position) {
        AppInfo info = null;
        synchronized (displayApps) {
            try {
                info = displayApps.get(position);
            } catch (Throwable ignored) {
            }
        }
        return info;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        AppInfo info = getAppInfo(position);
        if (info != null) {
            appClicked(info, v);
        }
    }

    public void appClicked(@NonNull AppInfo appInfo, View view) {
        PackageManager pm = getPackageManager();
        ApplicationInfo moreInfo;
        try {
            moreInfo = pm.getApplicationInfo(appInfo.packagePath, 0);
            String _apkPath = moreInfo.sourceDir;

            if (BuildConfig.PARSER_ONLY) {
                UserAppActivity.startFullEditActivity(this, _apkPath);
            } else if (BuildConfig.LIMIT_NEW_VERSION && !MainActivity.upgradedFromOldVersion(this)) {
                startFullEditActivity(this, _apkPath);
            } else {
                new EditModeView(this, this, _apkPath, moreInfo.packageName).showAppEditDialog(view);
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.btn_search) {
            searchApp();
        }
    }

    @SuppressLint("DefaultLocale")
    private void searchApp() {
        String keyword = keywordEdit.getText().toString();
        keyword = keyword.toLowerCase();

        List<AppInfo> matchedApps = new ArrayList<>();
        for (AppInfo appInfo : this.allApps) {
            if (appInfo.appName.toLowerCase().contains(keyword)) {
                matchedApps.add(appInfo);
            }
        }

        showAppList(matchedApps);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (info == null) {
            return;
        }

        AppInfo appInfo = getAppInfo(info.position);
        if (appInfo == null) {
            return;
        }

        menu.setHeaderTitle(appInfo.appName);

        menu.add(0, APPINFO_ID, 0, R.string.app_info);
        menu.add(0, BACKUP_ID, 0, R.string.backup);
        menu.add(0, LAUNCH_ID, 0, R.string.launch);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;

        switch (item.getItemId()) {
            case APPINFO_ID:
                showAppInfo(position);
                return true;
            case BACKUP_ID:
                backupApp(position);
                return true;
            case LAUNCH_ID:
                launchApp(position);
                return true;
        }

        return (super.onOptionsItemSelected(item));
    }

    // App Detail/information
    private void showAppInfo(int position) {
        try {
            AppInfo info = getAppInfo(position);
            if (info == null) {
                return;
            }

            String packageName = info.packagePath;

            try {
                // Open the specific App Info page:
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            } catch (ActivityNotFoundException ignored) {
                // Open the generic Apps page:
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                startActivity(intent);
            }
        } catch (Exception ignored) {
        }
    }

    // Copy apk of the target app to sd card
    private void backupApp(int position) {
        try {
            AppInfo info = getAppInfo(position);
            if (info == null) {
                return;
            }

            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(info.packagePath, 0);
            final String appName = info.appName;
            final String apkPath = appInfo.publicSourceDir;

            ProcessingDialog dlg = new ProcessingDialog(this, new ProcessingDialog.ProcessingInterface() {
                private String outPath;
                private boolean succeed = false;

                @Override
                public void process() throws Exception {
                    outPath = FileUtils.makeBackupDir(UserAppActivity.this) + appName + ".apk";

                    FileInputStream in = null;
                    FileOutputStream out = null;
                    try {
                        in = new FileInputStream(apkPath);
                        out = new FileOutputStream(outPath);
                        IOUtils.copy(in, out);
                        succeed = true;
                    } finally {
                        IOUtils.closeQuietly(in);
                        IOUtils.closeQuietly(out);
                    }
                }

                @Override
                public void afterProcess() {
                    if (succeed) {
                        String format = getString(R.string.apk_saved_tip);
                        String message = String.format(format, outPath);
                        Toast.makeText(UserAppActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                }
            }, -1);

            dlg.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchApp(int position) {
        try {
            AppInfo info = getAppInfo(position);
            if (info == null) {
                return;
            }

            if (!openApp(this, info.packagePath)) {
                String message = String.format(getString(R.string.cannot_launch), info.appName);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
        }
    }

    // Simple edit or full edit clicked
    @Override
    public void editModeSelected(int mode, String extraStr) {
        String filePath = extraStr;

        Intent intent = null;
        switch (mode) {
            case EditModeView.SIMPLE_EDIT:
                intent = new Intent(this, SimpleEditActivity.class);
                break;
            case EditModeView.FULL_EDIT:
                if (startFullEditActivity(this, filePath)) {
                    this.finish();
                }
                return;
            case EditModeView.COMMON_EDIT:
                intent = new Intent(this, CommonEditActivity.class);
                break;
            case EditModeView.XML_FILE_EDIT:
                intent = new Intent(this, AxmlEditActivity.class);
                break;
            case EditModeView.DATA_EDIT:
                Intent prefIntent = new Intent(this, PrefOverallActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("packagePath", extraStr);
                bundle.putBoolean("backup", false);
                prefIntent.putExtras(bundle);
                startActivity(prefIntent);
                break;
        }

        if (intent != null) {
            ActivityUtils.attachParam(intent, "apkPath", filePath);
            startActivity(intent);

            this.finish();
        }
    }


    // Async handler
    private static class MyHandler extends Handler {
        private final WeakReference<UserAppActivity> activityRef;

        private List<AppInfo> appList;

        public MyHandler(UserAppActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        public void setAppList(List<AppInfo> appList) {
            this.appList = appList;
        }

        public void handleMessage(Message msg) {
            UserAppActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                // Refresh apps list
                case 0:
                    activity.allApps.clear();
                    activity.allApps.addAll(appList);
                    activity.showAppList(appList);
                    break;
            }
        }
    }
}