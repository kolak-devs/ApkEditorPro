package com.balsikandar.crashreporter.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.balsikandar.crashreporter.R;
import com.balsikandar.crashreporter.utils.AppUtils;
import com.balsikandar.crashreporter.utils.FileUtils;

import java.io.File;
import java.net.URLConnection;

public class LogMessageActivity extends AppCompatActivity {

    private TextView appInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_message);
        appInfo = findViewById(R.id.appInfo);

        Intent intent = getIntent();
        if (intent != null) {
            String dirPath = intent.getStringExtra("LogMessage");
            File file = new File(dirPath);
            String crashLog = FileUtils.readFromFile(file);
            TextView textView = findViewById(R.id.logMessage);
            textView.setText(crashLog);
        }

        Toolbar myToolbar = findViewById(R.id.toolbar);
        myToolbar.setTitle(getString(R.string.crash_reporter));
        setSupportActionBar(myToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getAppInfo();
    }

    private void getAppInfo() {
        appInfo.setText(AppUtils.getDeviceDetails(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.crash_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = getIntent();
        String filePath = null;
        if (intent != null) {
            filePath = intent.getStringExtra("LogMessage");
        }

        if (item.getItemId() == R.id.delete_log) {
            if (FileUtils.delete(filePath)) {
                finish();
            }
            return true;
        } else if (item.getItemId() == R.id.share_crash_log) {
            shareCrashReport(filePath);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void shareCrashReport(String filePath) {
        Uri logUri = FileProvider.getUriForFile(
                this,
                "com.mcal.apkeditor.pro",
                new File(filePath));

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, appInfo.getText().toString());
        shareIntent.putExtra(Intent.EXTRA_STREAM, logUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, new File(filePath).getName());
        shareIntent.setType(URLConnection.guessContentTypeFromName(new File(filePath).getName()));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(shareIntent, "Share via..."));
    }
}
