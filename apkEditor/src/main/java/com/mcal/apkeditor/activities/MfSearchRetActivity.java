package com.mcal.apkeditor.activities;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.R;
import com.mcal.common.activities.CustomizedLangActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MfSearchRetActivity extends CustomizedLangActivity implements OnClickListener {
    private final ArrayList<EditText> editViews = new ArrayList<>();
    private String xmlPath;
    private ArrayList<Integer> lineIndexs;
    private ArrayList<String> lineContents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_mf_searchret);

        final Bundle bundle = getIntent().getExtras();
        this.xmlPath = bundle.getString("filePath");
        this.lineIndexs = bundle.getIntegerArrayList("lineIndexs");
        this.lineContents = bundle.getStringArrayList("lineContents");

        initView();
    }

    private void initView() {
        final String format = getResources().getString(R.string.mf_search_ret);
        final String title = String.format(format, lineIndexs.size());
        setupToolbar(R.id.toolbar, title, true);

        final Button saveBtn = (Button) findViewById(R.id.btn_save);
        saveBtn.setOnClickListener(this);
        final Button closeBtn = (Button) findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);

        final LinearLayout layout = (LinearLayout) findViewById(R.id.result_layout);
        final ArrayList<String> contents = lineContents;
        for (int i = 0; i < contents.size(); i++) {
            final EditText et = new EditText(this);
            et.setText(contents.get(i));
            et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            layout.addView(et);

            editViews.add(et);
        }
    }

    @Override
    public void onClick(@NonNull View v) {
        final int id = v.getId();
        if (id == R.id.btn_close) {
            finish();
        } else if (id == R.id.btn_save) {
            saveModification();
        }
    }

    private void saveModification() {
        boolean modified = false;
        final ArrayList<EditText> views = editViews;
        final ArrayList<String> contents = lineContents;
        // Collect the modification
        for (int i = 0; i < views.size(); i++) {
            final EditText et = views.get(i);
            final String newStr = et.getText().toString();
            final String oldStr = contents.get(i);
            if (!oldStr.equals(newStr)) {
                contents.set(i, newStr);
                modified = true;
            }
        }

        if (modified) {
            if (saveManifest()) {
                Toast.makeText(this, R.string.succeed, Toast.LENGTH_SHORT).show();
                // To indicate the manifest is modified
                setResult(1);
                finish();
            }
        } else {
            Toast.makeText(this, R.string.no_change_detected, Toast.LENGTH_SHORT).show();
        }
    }

    // The value is already collected before calling
    private boolean saveManifest() {
        boolean succeed = false;
        final String path = xmlPath;
        try {
            final FileOutputStream fos = new FileOutputStream(path + ".tmp");
            final FileInputStream fis = new FileInputStream(path);
            final BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            // Read all the contents
            final List<String> allContents = new ArrayList<>();
            String line = br.readLine();
            while (line != null) {
                allContents.add(line);
                line = br.readLine();
            }

            final ArrayList<Integer> indexes = lineIndexs;
            // Revise the content
            for (int i = 0; i < indexes.size(); i++) {
                int lineIndex = indexes.get(i) - 1;
                final String newStr = lineContents.get(i);
                final String oldStr = allContents.get(lineIndex);
                final String head = getHeadPadding(oldStr);
                allContents.set(lineIndex, head + newStr.trim());
            }

            // Save the new content
            for (String lineStr : allContents) {
                fos.write(lineStr.getBytes());
                fos.write('\n');
            }

            br.close();
            fis.close();
            fos.close();

            // Move temp file to overwrite the origin file
            new File(path + ".tmp").renameTo(new File(path));

            succeed = true;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return succeed;
    }

    // Get the head blanks
    @NonNull
    private String getHeadPadding(@NonNull String str) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            if (c == ' ' || c == '\t') {
                sb.append(c);
            } else {
                break;
            }
        }
        return sb.toString();
    }
}
