package com.gmail.heagoo.apkeditor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.prj.ProjectListActivity;
import com.gmail.heagoo.apkeditor.prj.ProjectListActivity2;
import com.gmail.heagoo.apkeditor.util.OnlineMessage;
import com.gmail.heagoo.common.FileUtil;
import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.httpserver.HttpServiceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        ProcessingDialog.ProcessingInterface {
    private static int g_vcRet;
    public static final boolean nativeLibLoaded = true;

    private OnlineMessage prompter;
    private int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;

    public static int isX86() {
        return NativeHelper.isX86();
    }

    public static void it(Object ctx, String pkgName, String dataDir, String apkPath) {
        if (ctx instanceof Context) {
            NativeHelper.it((Context) ctx, pkgName, dataDir, apkPath);
        }
    }

    public static void mg(String res, String orig,
                          String replaces, int len2, String mapping, int len1) {
        NativeHelper.mg(res, orig, replaces, len2, mapping, len1);
    }

    public static void md(String target, String source, String added,
                          int len1, String removed, int len2, String replaced, int len3) {
        NativeHelper.md(target, source, added, len1, removed, len2, replaced, len3);
    }

    public static int vc(Object ctx, int seed) {
        return NativeHelper.vc(null, seed);
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    public static boolean upgradedFromOldVersion(Context ctx) {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLanguage();
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initUI();

        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo moreInfo = pm.getApplicationInfo(this.getPackageName(), 0);
            String apkPath = moreInfo.sourceDir;
            it(this.getApplicationContext(),
                    this.getPackageName(), this.getFilesDir().getPath(), apkPath);
        } catch (Exception ignored) {
        }

        if (!BuildConfig.IS_PRO) {
            this.prompter = new OnlineMessage(this);
        }

        if (BuildConfig.SHOW_AGREEMENT) {
            if (!AppAgreementDialog.appLicenseAccepted(this)) {
                new AppAgreementDialog(this).show();
            } else {
                initFileWithPermissionCheck();
            }
        } else {
            initFileWithPermissionCheck();
        }
    }

    private void setupLanguage() {
        String languageToLoad =
                PreferenceManager.getDefaultSharedPreferences(this).getString("Language", "");
        if (!languageToLoad.equals("")) {
            Locale locale = new Locale(languageToLoad);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocale(locale);
            } else {
                config.locale = locale;
            }
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }
    }

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

    private void initUI() {
        MaterialButton openApkBtn = (MaterialButton) this.findViewById(R.id.tv_select_apkfile);
        openApkBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FileListActivity.class);
            startActivity(intent);
        });

        MaterialButton openAppBtn = (MaterialButton) this.findViewById(R.id.tv_select_appfile);
        openAppBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UserAppActivity.class);
            startActivity(intent);
        });

        MaterialButton settingsButton = (MaterialButton) this.findViewById(R.id.tv_settings);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            startActivity(intent);
        });

        MaterialButton exitButton = (MaterialButton) this.findViewById(R.id.tv_exit);
        exitButton.setOnClickListener(v -> {
            if (System.currentTimeMillis() < 3600 * 1000) {
                test();
            }
            new ProcessingDialog(MainActivity.this, MainActivity.this, -1).show();
        });

        MaterialButton helpButton = (MaterialButton) this.findViewById(R.id.tv_help);
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

    private void test() {
        g_vcRet = vc(this.getApplicationContext(), 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_projects) {
            Class<?> cls = (BuildConfig.PARSER_ONLY ? ProjectListActivity2.class : ProjectListActivity.class);
            Intent intent = new Intent(this, cls);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_img_downloader) {
            Intent intent = new Intent(this, ImageDownloadActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_about) {
            new AboutDialog(this).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initFile();
        }
    }

    public void initFileWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            initFile();
        }
    }

    private void initFile() {
        if (!BuildConfig.IS_PRO) {
            return;
        }
        if (BuildConfig.LIMIT_NEW_VERSION) {
            return;
        }

        File f = new File(this.getFilesDir(), "work.xml");
        if (!f.exists()) {
            try {
                f.createNewFile();
                f.setWritable(true);
            } catch (IOException ignored) {
            }
        }

        f = new File(this.getFilesDir(), "work.db");
        if (!f.exists()) {
            try {
                f.createNewFile();
                f.setWritable(true);
            } catch (IOException ignored) {
            }
        }

        try {
            File bin = new File(this.getFilesDir(), "mycp");
            if (!bin.exists() && (Build.VERSION.SDK_INT >= 20)) {
                InputStream input = getAssets().open("mycp");
                FileOutputStream output = new FileOutputStream(bin);
                IOUtils.copy(input, output);
                input.close();
                output.close();
                bin.setExecutable(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process() throws Exception {
        try {
            Intent intent = new Intent(MainActivity.this, ApkComposeService.class);
            stopService(intent);
            HttpServiceManager.instance().stopWebService(this);

            File fileDir = MainActivity.this.getFilesDir();
            String rootDirectory = fileDir.getAbsolutePath();
            String decodeRootPath = rootDirectory + "/decoded";
            FileUtil.deleteAll(new File(decodeRootPath));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void afterProcess() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
