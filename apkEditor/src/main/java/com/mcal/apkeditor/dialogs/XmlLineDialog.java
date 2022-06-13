package com.mcal.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

public class XmlLineDialog {
    private final IXmlLineChanged lineChangeListener;
    private final LinkedHashMap<String, String> keyValues;
    private final Context mContext;
    private final LinearLayout keyValueLayout;
    // Record all the value edit text
    private final List<EditText> valueEtList;
    private final boolean selfClosed;
    private String tag;
    // One line may contain several tags, content after first tag is put into
    // extraContent
    private String extraContent;

    @SuppressLint("CutPasteId")
    public XmlLineDialog(Context context, IXmlLineChanged changeListener,
                         int lineIndex, @NonNull String lineContent) {
        mContext = context;
        lineChangeListener = changeListener;

        int endPos = lineContent.indexOf('>');
        if (endPos != -1) {
            extraContent = lineContent.substring(endPos + 1);
            if (endPos != lineContent.length() - 1) {
                lineContent = lineContent.substring(0, endPos + 1);
            }
        }

        // Parse into detail information (tag, key/value, selfClosed)
        keyValues = new LinkedHashMap<>();
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

        View view = LayoutInflater.from(context).inflate(R.layout.dlg_xmlline, null);

        AlertDialog materialDialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .create();

        // Add key/values
        keyValueLayout = view.findViewById(R.id.view_keyvalue);
        valueEtList = new ArrayList<>();
        if (keyValues.isEmpty()) {
            View v = new View(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 100);
            v.setLayoutParams(lp);
            keyValueLayout.addView(v, 0);
            materialDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.ok), (dialog, which) -> dialog.dismiss());
        } else {
            int index = 0;
            for (Entry<String, String> entry : keyValues.entrySet()) {
                View child = LayoutInflater.from(context).inflate(R.layout.item_stringvalue, null);
                AppCompatTextView tv = child.findViewById(R.id.string_name);
                tv.setText(entry.getKey());
                AppCompatEditText valueEt = child.findViewById(R.id.string_value);
                valueEtList.add(valueEt);
                valueEt.setText(entry.getValue());
                keyValueLayout.addView(child, index++);
            }

            materialDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.save), (dialog, which) -> {
                if (lineChangeListener != null) {
                    lineChangeListener.xmlLineChanged(lineIndex, getLineData());
                }
                dialog.dismiss();
            });

            materialDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss());
        }
        materialDialog.show();

        addNewValue();
    }

    private void addNewValue() {
        final Context context = mContext;
        ImageView imageView = keyValueLayout.findViewById(R.id.hidden_image);
        imageView.setVisibility(View.VISIBLE);
        imageView.setOnClickListener(v -> {
            AlertDialog addValueDialog = new MaterialAlertDialogBuilder(context).create();

            View view1 = LayoutInflater.from(context).inflate(R.layout.dlg_addkeyvalue, null);

            addValueDialog.setTitle(R.string.add_key_value);
            addValueDialog.setView(view1);
            addValueDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(android.R.string.ok), (dialog, which) -> {
                EditText keyEt = view1.findViewById(R.id.key);
                EditText valueEt = view1.findViewById(R.id.value);
                String strKey = keyEt.getText().toString();
                strKey = strKey.trim();
                String strValue = valueEt.getText().toString();
                strValue = strValue.trim();
                if (strKey.equals("")) {
                    Toast.makeText(context, R.string.empty_key_tip, Toast.LENGTH_SHORT).show();
                } else {
                    keyValues.put(strKey, strValue);

                    View child = LayoutInflater.from(context).inflate(R.layout.item_stringvalue, null);
                    TextView tv = child.findViewById(R.id.string_name);
                    tv.setText(strKey);
                    EditText valueEdit = child.findViewById(R.id.string_value);
                    valueEtList.add(valueEdit);
                    valueEdit.setText(strValue);
                    keyValueLayout.addView(child, keyValues.size() - 1);
                }
                dialog.dismiss();
            });
            addValueDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), (dialog, which) -> {
                dialog.dismiss();
            });
            addValueDialog.show();
        });
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

    // Get modified line from UI
    @NonNull
    private String getLineData() {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(tag);
        int index = 0;
        for (Entry<String, String> entry : keyValues.entrySet()) {
            String key = entry.getKey();
            EditText et = valueEtList.get(index);
            String newValue = et.getText().toString();
            sb.append(" ").append(key).append("=\"").append(newValue).append("\"");
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
