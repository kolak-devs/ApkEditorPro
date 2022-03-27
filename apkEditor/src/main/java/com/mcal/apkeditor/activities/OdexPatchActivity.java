package com.mcal.apkeditor.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.dialogs.FileSelectDialog;
import com.mcal.apkeditor.dialogs.ProcessingDialog;
import com.mcal.apkeditor.utils.OdexPatcher;
import com.mcal.common.utils.ApkInfoParser;

import org.jetbrains.annotations.NotNull;

/**
 * Created by phe3 on 1/30/2018.
 */

public class OdexPatchActivity extends AppCompatActivity implements View.OnClickListener, FileSelectDialog.IFileSelection {
    private AppCompatEditText apkPathEt;
    private String apkPath;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.activity_odex_patch);
        setupToolbar("Odex Patcher");

        initView();
    }

    private void setupToolbar(String title) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initView() {
        this.apkPathEt = (AppCompatEditText) findViewById(R.id.et_apkpath);

        AppCompatButton selectBtn = (AppCompatButton) findViewById(R.id.btn_select_apkpath);
        selectBtn.setOnClickListener(this);
        AppCompatButton applyBtn = (AppCompatButton) findViewById(R.id.btn_apply_patch);
        applyBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.btn_select_apkpath) {
            new FileSelectDialog(this, this, ".apk", "", null);
        } else if (id == R.id.btn_apply_patch) {
            this.apkPath = apkPathEt.getText().toString();
            ProcessingDialog dlg = new ProcessingDialog(this, new PatchProcessor(), -1);
            dlg.show();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Processing Dialog

    @Override
    public void fileSelectedInDialog(String filePath, String extraStr, boolean openFile) {
        apkPathEt.setText(filePath);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // File selection

    @Override
    public boolean isInterestedFile(@NonNull String filename, String extraStr) {
        return filename.endsWith(".apk");
    }

    @Override
    public String getConfirmMessage(String filePath, String extraStr) {
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class PatchProcessor implements ProcessingDialog.ProcessingInterface {
        private String errMessage;
        private String odexPath;

        @Override
        public void process() throws Exception {
            ApkInfoParser parser = new ApkInfoParser();
            ApkInfoParser.AppInfo info = parser.parse(OdexPatchActivity.this, apkPath);
            if (info == null) {
                return;
            }

            String packageName = info.packageName;
            OdexPatcher patcher = new OdexPatcher(packageName);
            patcher.applyPatch(OdexPatchActivity.this, apkPath);

            odexPath = patcher.targetOdex;
            if (patcher.errMessage != null) {
                this.errMessage = patcher.errMessage;
                throw new Exception(errMessage);
            }
        }

        @Override
        public void afterProcess() {
            if (errMessage == null) {
                Toast.makeText(OdexPatchActivity.this, "Patched to " + odexPath, Toast.LENGTH_LONG).show();
            }
        }
    }
}
