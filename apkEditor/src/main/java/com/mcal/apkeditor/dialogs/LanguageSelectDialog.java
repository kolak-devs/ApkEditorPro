package com.mcal.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.ApkInfoActivity;
import com.mcal.apkeditor.R;
import com.mcal.androlib.LanguageMapping;
import com.mcal.apkeditor.view.ViewDialog;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class LanguageSelectDialog implements
        android.view.View.OnClickListener {

    private final WeakReference<ApkInfoActivity> activityRef;
    private final View contentView;
    private final EditText codeEt;
    private final boolean isAutoTranslate;
    private final ViewDialog dialog;
    private String[] codes;
    private String[] languages;

    @SuppressLint("InflateParams")
    public LanguageSelectDialog(ApkInfoActivity activity, String[] _lang, String[] _codes, String resId) {

        this.languages = _lang;
        this.codes = _codes;
        this.isAutoTranslate = (languages != null);

        this.activityRef = new WeakReference<>(activity);
        this.contentView = activity.getLayoutInflater().inflate(
                R.layout.dlg_selectlanguage, null, false);

        dialog = new ViewDialog(activity);
        dialog.setTitle(resId);
        dialog.setView(contentView);
        dialog.show();

        this.codeEt = contentView.findViewById(R.id.language_code);
        if (isAutoTranslate) { // Do not allow to modify
            codeEt.setEnabled(false);
        }

        initSpinner();
        initButton();
    }

    private void initButton() {
        Button okBtn = contentView.findViewById(R.id.btn_addlang_ok);
        okBtn.setOnClickListener(this);

        Button cancelBtn = contentView
                .findViewById(R.id.btn_addlang_cancel);
        cancelBtn.setOnClickListener(this);
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
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
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

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.btn_addlang_ok) {
            String strCode = codeEt.getText().toString();
            if (this.isAutoTranslate) {
                translateLanguage(strCode);
                dialog.dismiss();
            } else {
                if (addLanguage(strCode)) {
                    dialog.dismiss();
                }
            }
        } else if (id == R.id.btn_addlang_cancel) {
            dialog.dismiss();
        }
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
