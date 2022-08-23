package com.mcal.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.mcal.androlib.LanguageMapping;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.activities.ApkInfoActivity;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class LanguageSelectDialog {
    private final WeakReference<ApkInfoActivity> activityRef;
    private final View contentView;
    private final TextInputEditText codeEt;
    private final boolean isAutoTranslate;
    private final AlertDialog materialDialog;
    private String[] codes;
    private String[] languages;

    @SuppressLint("InflateParams")
    public LanguageSelectDialog(ApkInfoActivity activity, String[] _lang, String[] _codes) {
        languages = _lang;
        codes = _codes;
        isAutoTranslate = (languages != null);

        this.activityRef = new WeakReference<>(activity);
        this.contentView = activity.getLayoutInflater().inflate(R.layout.dialog_selectlanguage, null, false);
        codeEt = contentView.findViewById(R.id.language_code);
        if (isAutoTranslate) { // Do not allow to modify
            codeEt.setEnabled(false);
        }
        materialDialog = new MaterialAlertDialogBuilder(activity)
                .setView(contentView)
                .create();
        materialDialog.setButton(DialogInterface.BUTTON_POSITIVE, activity.getString(android.R.string.ok), (dialog, which) -> {
            String strCode = codeEt.getText().toString();
            if (isAutoTranslate) {
                translateLanguage(strCode);
                materialDialog.dismiss();
            } else {
                if (addLanguage(strCode)) {
                    materialDialog.dismiss();
                }
            }
            dialog.dismiss();
        });

        materialDialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss());
        materialDialog.show();

        initSpinner();
    }

    private void initSpinner() {
        int size = LanguageMapping.getSize();
        if (this.codes == null || this.languages == null) {
            this.codes = new String[size];
            this.languages = new String[size];
            LanguageMapping.getLanguages(codes, languages);
        }

        // Initialize spinner by setting adapter
        Spinner spinner = contentView.findViewById(R.id.language_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                activityRef.get(), android.R.layout.simple_spinner_item,
                languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Set the default value of language
        Locale locale = Locale.getDefault();
        String code = "-" + locale.getLanguage();
        int selected = getCodeIndex(code);
        if (selected != -1) {
            spinner.setSelection(selected);
        }

        // Event listener
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int position, long arg3) {
                updateLanguageCode(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

    }

    private int getCodeIndex(String code) {
        int idx = -1;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].startsWith(code)) {
                idx = i;
                break;
            }
        }
        return idx;
    }

    protected void updateLanguageCode(int position) {
        codeEt.setText(codes[position]);
    }

    // Add a language
    private boolean addLanguage(String strCode) {
        ApkInfoActivity activity = activityRef.get();
        String error = activity.addLanguageRetError(strCode);
        if (error == null) {
            return true;
        } else {
            Toast.makeText(activity, error, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // Translate to target language
    private void translateLanguage(String strCode) {
        ApkInfoActivity activity = activityRef.get();
        activity.translateLanguage(strCode);
    }
}