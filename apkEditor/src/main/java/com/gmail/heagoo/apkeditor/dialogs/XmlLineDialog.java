package com.gmail.heagoo.apkeditor.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;

import com.gmail.heagoo.apkeditor.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import ru.svolf.melissa.sheet.ViewDialog;

public class XmlLineDialog implements
        android.view.View.OnClickListener {

    // private Context ctx;
    private final IXmlLineChanged lineChangeListener;
    private final int lineIndex;
    private final LinkedHashMap<String, String> keyValues;
    private final Context ctx;
    private final LinearLayout keyValueLayout;
    private final ViewDialog dialog;
    // Record all the value edit text
    List<EditText> valueEtList;
    private String tag;
    private boolean selfClosed = false;
    // One line may contain several tags, content after first tag is put into
    // extraContent
    private String extraContent;
    // Dialog to add a key/value
    private ViewDialog keyValueDlg;
    private View keyValueView;

    public XmlLineDialog(Context ctx, IXmlLineChanged changeListener,
                         int lineIndex, @NonNull String lineContent) {
        this.ctx = ctx;
        this.lineChangeListener = changeListener;
        this.lineIndex = lineIndex;

        int endPos = lineContent.indexOf('>');
        if (endPos != -1) {
            extraContent = lineContent.substring(endPos + 1);
            if (endPos != lineContent.length() - 1) {
                lineContent = lineContent.substring(0, endPos + 1);
            }
        }

        // Parse into detail information (tag, key/value, selfClosed)
        this.keyValues = new LinkedHashMap<String, String>();
        String[] words = lineContent.split(" ");
        tag = words[0].trim();
        if (tag.startsWith("<")) {
            tag = tag.substring(1);
        } else {
            tag = "";
        }
        for (int i = 1; i < words.length; i++) {
            String[] segs = words[i].split("=");
            if (segs.length == 2) {
                String value = trimValue(segs[1]);
                if (value != null) {
                    keyValues.put(segs[0], value);
                }
            }
        }
        selfClosed = lineContent.endsWith("/>");

        View view = LayoutInflater.from(ctx).inflate(R.layout.dlg_xmlline, null);
        Button closeBtn = view.findViewById(R.id.btn_dlgclose);
        closeBtn.setOnClickListener(this);
        Button saveBtn = view.findViewById(R.id.btn_dlgsave);
        saveBtn.setOnClickListener(this);

        // Add key/values
        this.keyValueLayout = view
                .findViewById(R.id.view_keyvalue);
        valueEtList = new ArrayList<>();
        if (keyValues.isEmpty()) {
            View v = new View(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, 100);
            v.setLayoutParams(lp);
            keyValueLayout.addView(v, 0);
            saveBtn.setVisibility(View.GONE);
        } else {
            int index = 0;
            for (Entry<String, String> entry : keyValues.entrySet()) {
                View child = LayoutInflater.from(ctx).inflate(R.layout.item_stringvalue,
                        null);
                AppCompatTextView tv = child.findViewById(R.id.string_name);
                tv.setText(entry.getKey());
                AppCompatEditText valueEt = child
                        .findViewById(R.id.string_value);
                valueEtList.add(valueEt);
                valueEt.setText(entry.getValue());
                keyValueLayout.addView(child, index++);
            }

            // Add Image
            ImageView imageView = keyValueLayout
                    .findViewById(R.id.hidden_image);
            imageView.setVisibility(View.VISIBLE);
            imageView.setOnClickListener(this);
        }

        dialog = new ViewDialog(ctx);
        dialog.setTitle(lineContent);
        dialog.setView(view);
    }

    public void show() {
        dialog.show();
    }

    // Trim the comma, if it ends with >, also trim it
    @Nullable
    private String trimValue(@NonNull String strValue) {
        if (strValue.startsWith("\"")) {
            if (strValue.endsWith("\"")) {
                return strValue.substring(1, strValue.length() - 1);
            } else if (strValue.endsWith("\">")) {
                return strValue.substring(1, strValue.length() - 2);
            }
        }
        return null;
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        // Close
        if (id == R.id.btn_dlgclose) {
            dialog.dismiss();
        }
        // Save
        else if (id == R.id.btn_dlgsave) {
            if (lineChangeListener != null) {
                lineChangeListener.xmlLineChanged(lineIndex, getLineData());
            }
            dialog.dismiss();
        }
        // To add a key/value to this line
        else if (id == R.id.hidden_image) {
            this.keyValueDlg = new ViewDialog(ctx);

            View view = LayoutInflater.from(ctx).inflate(R.layout.dlg_addkeyvalue, null);
            Button okBtn = view.findViewById(R.id.btn_addkeyvalue_ok);
            okBtn.setOnClickListener(this);
            Button cancelBtn = view
                    .findViewById(R.id.btn_addkeyvalue_cancel);
            cancelBtn.setOnClickListener(this);
            this.keyValueView = view;
            keyValueDlg.setTitle(R.string.add_key_value);
            keyValueDlg.setView(view);
            keyValueDlg.show();
        }
        // OK button clicked in key/value dialog
        else if (id == R.id.btn_addkeyvalue_ok) {
            EditText keyEt = keyValueView.findViewById(R.id.key);
            EditText valueEt = keyValueView.findViewById(R.id.value);
            String strKey = keyEt.getText().toString();
            strKey = strKey.trim();
            String strValue = valueEt.getText().toString();
            strValue = strValue.trim();
            if (strKey.equals("")) {
                Toast.makeText(ctx, R.string.empty_key_tip, Toast.LENGTH_SHORT)
                        .show();
            } else {
                // LOGGER.info("key=" + strKey + ", value=" + strValue);
                keyValues.put(strKey, strValue);

                View child = LayoutInflater.from(ctx).inflate(R.layout.item_stringvalue,
                        null);
                TextView tv = child.findViewById(R.id.string_name);
                tv.setText(strKey);
                EditText valueEdit = child
                        .findViewById(R.id.string_value);
                valueEtList.add(valueEdit);
                valueEdit.setText(strValue);
                keyValueLayout.addView(child, keyValues.size() - 1);

                keyValueDlg.dismiss();
            }
        }
        // Cancel button clicked in key/value dialog
        else if (id == R.id.btn_addkeyvalue_cancel) {
            keyValueDlg.dismiss();
        }
    }

    // Get modified line from UI
    @NonNull
    private String getLineData() {
        StringBuilder sb = new StringBuilder();
        sb.append("<" + tag);
        int index = 0;
        for (Entry<String, String> entry : keyValues.entrySet()) {
            String key = entry.getKey();
            EditText et = valueEtList.get(index);
            String newValue = et.getText().toString();
            sb.append(" " + key + "=\"" + newValue + "\"");
            index++;
        }
        if (selfClosed) {
            sb.append(" />");
        } else {
            sb.append(">");
        }

        sb.append(extraContent);

        return sb.toString();
    }

    // Callback
    public interface IXmlLineChanged {
        void xmlLineChanged(int lineIndex, String newLine);
    }
}
