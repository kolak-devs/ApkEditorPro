package com.mcal.apkeditor.pro.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.pro.fragments.SettingsEditorFragment;
import com.mcal.common.activities.CustomizedLangActivity;
import com.mcal.common.data.Preferences;

import org.jetbrains.annotations.NotNull;

public class SettingsEditorActivity extends CustomizedLangActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Preferences.getFullScreen()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController insetsController = getWindow().getInsetsController();
                if (insetsController != null) {
                    insetsController.hide(WindowInsets.Type.statusBars());
                }
            } else {
                getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                );
            }
        }

        setContentView(R.layout.activity_settings);

        setupToolbar(getString(R.string.settings));

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.frame_container, new SettingsEditorFragment())
                .commit();
    }

    @SuppressWarnings("ConstantConditions")
    private void setupToolbar(String title) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (title != null) {
            getSupportActionBar().setTitle(title);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        // Respond to the action bar's Up/Home button
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
