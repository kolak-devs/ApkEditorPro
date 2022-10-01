package com.mcal.apkeditor.dialogs;

import static com.mcal.common.utils.FileHelperKt.copyFile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.BuildConfig;
import com.mcal.apkeditor.R;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class SmaliCodeDialog extends Dialog
        implements android.view.View.OnClickListener {
    static int[] smaliNameIds = {R.string.show_a_toast, R.string.log_a_message,
            R.string.dump_a_value, R.string.print_stack_trace};
    static String[] smaliCodes = {
            "    const-string v0, \"This is a toast.\"\n"
                    + "    # p0 (this object) must be an object of Context\n"
                    + "    invoke-static {p0, v0}, Lapkeditor/Utils;->showToast(Landroid/content/Context;Ljava/lang/String;)V",
            "    # use 'adb logcat APKEDITOR:* *:S' to view the log\n"
                    + "    const-string v0, \"I am here.\"\n"
                    + "    invoke-static {v0}, Lapkeditor/Utils;->log(Ljava/lang/String;)V",
            "    # use 'adb logcat APKEDITOR:* *:S' to view the value\n"
                    + "    invoke-static {v0}, Lapkeditor/Utils;->dumpValue(Ljava/lang/Object;)V",
            "    # use 'adb logcat APKEDITOR:* *:S' to view the stack trace\n"
                    + "    invoke-static {}, Lapkeditor/Utils;->printCallStack()V",
//            "    # 0x60 is the offset, change it to get a different IMEI\n" +
//                    "    const/16 v0, 0x60\n" +
//                    "    invoke-static {v0}, Lapkeditor/Utils;->generateImei(I)Ljava/lang/String;\n" +
//                    "    move-result-object v0",
    };
    private final WeakReference<Activity> activityRef;
    private final String smaliRootFolder;
    private EditText codeEt;

    // filePath: The path for current editing file
    public SmaliCodeDialog(Activity activity, String filePath) {
        super(activity);

        this.activityRef = new WeakReference<>(activity);
        this.smaliRootFolder = getSmaliRootFolder(filePath);

        init(activity);
    }

    @NonNull
    private String getSmaliRootFolder(@NonNull String filePath) {
        String[] dirs = filePath.split("/");
        StringBuilder smaliPath = new StringBuilder();
        for (String dir : dirs) {
            smaliPath.append(dir).append("/");
            if ("smali".equals(dir) || dir.startsWith("smali_")) {
                break;
            }
        }
        return smaliPath.toString();
    }

    @SuppressLint("InflateParams")
    private void init(final Activity activity) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_smalicode, null);

        // Spinner
        Spinner spinner = view.findViewById(R.id.spinner_codename);
        String[] names = new String[smaliNameIds.length];
        for (int i = 0; i < smaliNameIds.length; ++i) {
            names[i] = activity.getString(smaliNameIds[i]);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Event listener
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int position, long arg3) {
                updateSmaliCode(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        // Code Content
        codeEt = view.findViewById(R.id.et_samplecode);

        // Copy button
        Button copyBtn = view.findViewById(R.id.btn_copy);
        copyBtn.setOnClickListener(this);

        // Close button
        Button closeBtn = view.findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(this);

        setTitle("Smali Code");
        setContentView(view);
    }

    protected void updateSmaliCode(int position) {
        if (position < smaliCodes.length) {
            codeEt.setText(smaliCodes[position]);
        }
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.btn_close) {
            dismiss();
        } else if (id == R.id.btn_copy) {
            // Copy to clipboard
            Activity activity = activityRef.get();
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("code", codeEt.getText().toString());
            clipboard.setPrimaryClip(clip);

            // Copy Utils.smali to some folder
            copyUtilSmali();

            // Toast
            Toast.makeText(activity, R.string.smali_copied, Toast.LENGTH_SHORT).show();
        }
    }

    // Copy from assets to decode smali folder
    private void copyUtilSmali() {
        String dirPath = this.smaliRootFolder + "apkeditor";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdir();
        }

        String filePath = dirPath + "/Utils.smali";
        File file = new File(filePath);
        if (!file.exists()) {
            FileOutputStream fos = null;
            InputStream is = null;
            try {
                fos = new FileOutputStream(file);
                AssetManager am = activityRef.get().getAssets();
                is = am.open("smali_patch/Utils.smali");
                copyFile(is, fos);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeQuietly(fos);
                closeQuietly(is);
            }
        }
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
