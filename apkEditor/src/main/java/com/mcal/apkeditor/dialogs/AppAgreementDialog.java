package com.mcal.apkeditor.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mcal.apkeditor.MainActivity;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.view.ViewDialog;

import java.lang.ref.WeakReference;

public class AppAgreementDialog {
    private final WeakReference<MainActivity> activityRef;

    @SuppressLint("SetTextI18n")
    public AppAgreementDialog(MainActivity activity) {

        this.activityRef = new WeakReference<>(activity);

        LayoutInflater inflater = LayoutInflater.from(activity);
        View layout = inflater.inflate(R.layout.dlg_app_license, null);
        TextView tv = (TextView) layout.findViewById(R.id.tv_content);
        tv.setText("Information in this dialog is provided in connection with APK Editor. No license, express or implied, by estoppel or otherwise, to any intellectual property rights is granted by this.\n" +
                "\n" +
                "APK Editor is designed for Android fans who know what exactly they are doing, but not intended for hack, please use it under following terms:\n" +
                "\n" +
                "1) Please only modify the apk files which you have intellectual property rights.\n" +
                "\n" +
                "2) For apk files you don't have intellectual property rights, you need to ask for authorities from the developer to modify it. And even though you have rights to modify it, you still need to ask for re-distribution rights to publish it.\n" +
                "\n" +
                "3) To prevent abuse of APK Editor, sign feature is not provided any more.\n" +
                "\n" +
                "4) We may make changes to specifications and product descriptions at any time, without notice.");

        EditText inputEt = (EditText) layout.findViewById(R.id.et_input);

        ViewDialog dialog = new ViewDialog(activity);
        dialog.setTitle("Agreement");
        dialog.setView(layout);
        dialog.setCancelable(false);
        dialog.setPositive(android.R.string.ok, view -> {
            String input = inputEt.getText().toString();
            if (input.trim().toLowerCase().equals("accept")) {
                activityRef.get().initFileWithPermissionCheck();

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activityRef.get());
                SharedPreferences.Editor e = sp.edit();
                e.putBoolean("app_agreement_accepted", true);
                e.apply();

                dialog.dismiss();
            } else {
                Toast.makeText(activityRef.get(), R.string.input_agree_toast, Toast.LENGTH_SHORT).show();
                activityRef.get().finish();
            }
        });
        dialog.setNegative(android.R.string.cancel, view -> {
            activityRef.get().finish();
        });
        dialog.show();
    }

    public static boolean appLicenseAccepted(Context ctx) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getBoolean("app_agreement_accepted", false);
    }
}
