package com.mcal.apkeditor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.common.colormixer.ColorMixer;
import com.common.colormixer.ColorMixerDialog;
import com.common.colormixer.ColorValue;
import com.common.colormixer.ColorValueAdapter;
import com.mcal.common.activities.CustomizedLangActivity;
import com.mcal.common.utils.ActivityUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ColorXmlActivity extends CustomizedLangActivity
        implements OnClickListener, OnItemClickListener {
    private static final String ENTRYNAME = "res/values/colors.xml";

    private String xmlPath;
    private ArrayList<ColorValue> colorValues;
    private ColorValueAdapter colorAdapter;

    private Button saveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_colors_xml);

        this.xmlPath = ActivityUtils
                .getParam(getIntent(), "xmlPath");

        initData();

        initView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 0: // XmlActivity
                if (resultCode != 0) {
                    initData();
                    updateView();
                    setResult();
                }
        }
    }

    private void initView() {
        TextView filenameTv = this.findViewById(R.id.filename);
        filenameTv.setText(getFileName());

        this.colorAdapter = new ColorValueAdapter(this, colorValues);
        ListView colorLv = this.findViewById(R.id.color_list);
        colorLv.setAdapter(colorAdapter);
        colorLv.setOnItemClickListener(this);

        this.saveBtn = this.findViewById(R.id.btn_save);
        saveBtn.setOnClickListener(this);
        Button closeBtn = this.findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);
        Button openBtn = this.findViewById(R.id.btn_openeditor);
        openBtn.setOnClickListener(this);
    }

    private void updateView() {
        colorAdapter.updateData(this.colorValues);
    }

    @NonNull
    private String getFileName() {
        int pos = this.xmlPath.lastIndexOf('/');
        return xmlPath.substring(pos + 1);
    }

    private void initData() {
        this.colorValues = new ArrayList<ColorValue>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(new File(xmlPath)));
            String line = br.readLine();
            while (line != null) {
                ColorValue value = parseLine(line);
                if (value != null) {
                    this.colorValues.add(value);
                }
                line = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Parse the reference value
        for (int i = 0; i < colorValues.size(); ++i) {
            colorValues.get(i).parseRefColor(this, colorValues);
        }
    }

    // Parse a line like: <color name="white">#ffffffff</color>
    private ColorValue parseLine(@NonNull String line) {
        ColorValue result = null;

        final String tag = "<color name=\"";
        int position = line.indexOf(tag);
        if (position != -1) {
            int startPos = position + tag.length();
            int endPos = line.indexOf("\">", startPos);
            if (endPos != -1) {
                String name = line.substring(startPos, endPos);
                startPos = endPos + 2;
                endPos = line.indexOf("</color>", startPos);
                if (endPos != -1) {
                    String strValue = line.substring(startPos, endPos);
                    result = new ColorValue(name, strValue);
                }
            }
        }

        return result;
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            this.finish();
        } else if (id == R.id.btn_save) {
            saveColors();
            this.finish();
        } else if (id == R.id.btn_openeditor) {
            openInEditor();
        }
    }

    private void openInEditor() {
        String syntaxFileName = "xml.xml";
        String displayFileName = "colors.xml";

        Intent intent = TextEditor.getEditorIntent(this, this.xmlPath, null);
        ActivityUtils.attachParam(intent,
                "syntaxFileName", syntaxFileName);
        if (displayFileName != null) {
            ActivityUtils.attachParam(intent,
                    "displayFileName", displayFileName);
        }
        ActivityUtils.attachParam(intent, "extraString",
                ENTRYNAME);
        startActivityForResult(intent, 0);
    }

    private void saveColors() {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(xmlPath));
            bw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            bw.write("<resources>\n");
            for (int i = 0; i < this.colorValues.size(); ++i) {
                bw.write(colorValues.get(i).toString());
                bw.write("\n");
            }
            bw.write("</resources>");

            setResult();
        } catch (Exception e) {
            String fmt = getString(R.string.general_error);
            String message = String.format(fmt, e.getMessage());
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Make parent activity aware the modification
    private void setResult() {
        // Set modified flag as the result
        Intent intent = new Intent();
        intent.putExtra("xmlPath", xmlPath);
        intent.putExtra("extraString", ENTRYNAME);
        this.setResult(1, intent);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view,
                            final int position, long id) {
        int initColor = colorValues.get(position).intColorValue;
        ColorMixerDialog dialog = new ColorMixerDialog(this, initColor,
                new ColorMixer.OnColorChangedListener() {

                    @Override
                    public void onColorChange(int argb) {
                        colorChanged(position, argb);
                    }

                });
        dialog.show();
    }

    // The color is changed through the color dialog
    protected void colorChanged(int position, int argb) {
        if (position < colorValues.size()) {
            ColorValue value = colorValues.get(position);
            value.intColorValue = argb;
            value.strColorValue = "#" + Integer.toHexString(argb);

            this.saveBtn.setVisibility(View.VISIBLE);
            colorAdapter.notifyDataSetChanged();
        }
    }
}
