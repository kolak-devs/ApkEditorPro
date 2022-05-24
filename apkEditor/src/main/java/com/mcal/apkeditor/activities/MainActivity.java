package com.mcal.apkeditor.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.balsikandar.crashreporter.ui.CrashReporterActivity;
import com.mcal.apkeditor.ApkComposeService;
import com.mcal.apkeditor.BuildConfig;
import com.mcal.apkeditor.GlobalConfig;
import com.mcal.apkeditor.MenuListAdapter;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.data.Dialogs;
import com.mcal.apkeditor.dialogs.AppAgreementDialog;
import com.mcal.apkeditor.dialogs.ProcessingDialog;
import com.mcal.apkeditor.prj.ProjectListActivity;
import com.mcal.apkeditor.prj.ProjectListActivity2;
import com.mcal.apkeditor.utils.OnlineMessage;
import com.mcal.common.activities.CustomizedLangActivity;
import com.mcal.common.data.Preferences;
import com.mcal.common.utils.FileUtils;
import com.mcal.httpserver.HttpServiceManager;

import java.io.File;

/**
 * For apktool, look into:
 * brut.apktool\apktool-lib\src\main\java\brut\androlib\ApkDecoder.java:decode()
 * <p>
 * And then look into: brut.androlib.res.AndrolibResources
 */
public class MainActivity extends CustomizedLangActivity implements
        AdapterView.OnItemClickListener, ProcessingDialog.ProcessingInterface {

    // Native library
    static {
        System.loadLibrary("apkeditorpro");
    }

    // Used to show a dialog
    private OnlineMessage prompter;
    // For left side menu
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    // modify a zip file
    public static native void modifyZip(String target, String source, String added,
                                 int len1, String removed, int len2, String replaced, int len3);

    // Verify certificate
    // When report error of "Cannot register", to make sure it is called
    //public static native int vc(Object ctx, int seed);

    public static boolean upgradedFromOldVersion(@NonNull Context ctx) {
        File f = new File(ctx.getFilesDir(), "work.xml");
        return !f.exists();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.activity_main);
        setupToolbar(getString(R.string.app_name));

        initUI();

        // As pro has no network access, cannot get online message
        if (!BuildConfig.IS_PRO) {
            this.prompter = new OnlineMessage(this);
        }

        // Show license dialog
        if (BuildConfig.SHOW_AGREEMENT) {
            if (!AppAgreementDialog.appLicenseAccepted(this)) {
                new AppAgreementDialog(this);
            } else {
                initFileWithPermissionCheck();
            }
        } else {
            initFileWithPermissionCheck();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void setupToolbar(String title) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        if (!BuildConfig.IS_PRO) {
            prompter.showMessageDialog();
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setupSlidingMenu() {
        // enabling action bar app icon and behaving it as toggle button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerList = findViewById(R.id.slider_list);

        MenuListAdapter adapter = new MenuListAdapter(this);
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(this);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        );
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    private void initUI() {
        if (BuildConfig.PARSER_ONLY) {
            AppCompatImageView imageView = findViewById(R.id.logo);
            imageView.setImageResource(R.drawable.parser_logo);
        }

        // Left sliding menu
        setupSlidingMenu();

        // Select apk from folder
        AppCompatButton openApkBtn = this.findViewById(R.id.tv_select_apkfile);
        openApkBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FileListActivity.class);
            startActivity(intent);
        });

        // Select apk from app
        AppCompatButton openAppBtn = this.findViewById(R.id.tv_select_appfile);
        if (BuildConfig.DISPLAY_APP) {
            openAppBtn.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, UserAppActivity.class);
                startActivity(intent);
            });
        } else {
            openAppBtn.setText(R.string.settings);
            openAppBtn.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        }

        // Odex Patcher
        AppCompatButton odexPatcherBtn = this.findViewById(R.id.tv_odex_patcher);
        odexPatcherBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OdexPatchActivity.class);
            startActivity(intent);
        });


        // Exit
        AppCompatButton exitButton = this.findViewById(R.id.tv_exit);
        exitButton.setOnClickListener(v -> new ProcessingDialog(MainActivity.this, MainActivity.this, -1).show());

        // Help
        // For APK Parser, use it as 'project'
        AppCompatButton helpButton = this.findViewById(R.id.tv_help);
        if (BuildConfig.PARSER_ONLY) {
            helpButton.setText(R.string.projects);
            helpButton.setOnClickListener(v -> {
                Class<?> cls = (BuildConfig.PARSER_ONLY ? ProjectListActivity2.class : ProjectListActivity.class);
                Intent helpIntent = new Intent(MainActivity.this, cls);
                startActivity(helpIntent);
            });
        } else {
            helpButton.setOnClickListener(v -> {
                Intent helpIntent = new Intent(MainActivity.this, HelpActivity.class);
                startActivity(helpIntent);
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // toggle nav drawer on selecting action bar app icon/title
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_about) {
            //Dialogs.about(this);
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_night_mode) {
            if (Preferences.isNightModeEnabled()) {
                Preferences.setNightModeEnabled(false);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                getDelegate().applyDayNight();
            } else {
                Preferences.setNightModeEnabled(true);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                getDelegate().applyDayNight();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /***
     * Called when invalidateOptionsMenu() is triggered
     */
    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        // if nav drawer is opened, hide the action items
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
        menu.findItem(R.id.action_about).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initFile();
        }
    }

    public void initFileWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            initFile();
        }
    }

    private void initFile() {
        // For free version, do not need to create these files
        if (!BuildConfig.IS_PRO) {
            return;
        }

        // If need to limit the new version, does not need to create such files
        if (BuildConfig.LIMIT_NEW_VERSION) {
            return;
        }
    }

    // Click on the menu item
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        switch ((int) id) {
            case MenuListAdapter.ITEM_PROJECT: {
                Class<?> cls = (BuildConfig.PARSER_ONLY ? ProjectListActivity2.class : ProjectListActivity.class);
                Intent intent = new Intent(this, cls);
                startActivity(intent);
                break;
            }
            case MenuListAdapter.ITEM_SETTING: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            }
            case MenuListAdapter.ITEM_IMG_DOWNLOADER: {
                Intent intent = new Intent(this, ImageDownloadActivity.class);
                startActivity(intent);
                break;
            }
            case MenuListAdapter.ITEM_ABOUT: {
                Dialogs.about(this);
                break;
            }
            case MenuListAdapter.ITEM_LOGS: {
                Intent intent = new Intent(this, CrashReporterActivity.class);
                startActivity(intent);
                break;
            }
            case MenuListAdapter.ITEM_FORUM: {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://forum.timscriptov.ru/index.php?apkeditor-pro.9/")));
                break;
            }
            case MenuListAdapter.ITEM_TELEGRAM: {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/apkeditor2021")));
                break;
            }
        }
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    // Deal with exit
    @Override
    public void process() throws Exception {
        try {
            Intent intent = new Intent(MainActivity.this, ApkComposeService.class);
            stopService(intent);
            HttpServiceManager.instance().stopWebService(this);

            File fileDir = MainActivity.this.getFilesDir();
            String rootDirectory = fileDir.getAbsolutePath();
            String decodeRootPath = rootDirectory + "/decoded";
            FileUtils.deleteAll(new File(decodeRootPath));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    // Deal with exit
    @Override
    public void afterProcess() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
