package com.mcal.apkeditor.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.mcal.apkeditor.R;
import com.mcal.common.activities.CustomizedLangActivity;

public class AboutActivity extends CustomizedLangActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setupToolbar(R.string.about);
    }

    @SuppressWarnings("ConstantConditions")
    private void setupToolbar(@StringRes int title) {
        @NonNull
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        @NonNull
        ActionBar bar = getSupportActionBar();
        bar.setTitle(title);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowHomeEnabled(true);
    }

    public void openTelegram(@NonNull View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/apkeditor2021")));
    }

    public void openGitHub(@NonNull View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TimScriptov/ApkEditor")));
    }

    public void donateTon(@NonNull View view) {
        @NonNull
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        @NonNull
        ClipData clip = ClipData.newPlainText("Copied TON Coin Address", "EQBw0AcMsqJ7slxDD8u8bo2frsqWizASHLHmlNkte6giZWBE");
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Copied TON Coin Address", Toast.LENGTH_SHORT).show();
    }

    public void donatePayPal(@NonNull View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/timscriptov")));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
