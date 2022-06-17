package com.mcal.apkeditor.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import com.mcal.apkeditor.GlobalConfig;
import com.mcal.apkeditor.R;
import com.mcal.apkeditor.activities.TextEditNormalActivity;
import com.mcal.apkeditor.utils.Smali2Html;
import com.mcal.apkeditor.utils.ValuesXml2Html;
import com.mcal.apkeditor.utils.Xml2Html;
import com.mcal.common.data.Preferences;
import com.mcal.common.utils.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class HtmlViewDialog extends Dialog implements
        View.OnClickListener, SmaliMethodsDialogs.ISmaliMethodClicked {

    private final WeakReference<Activity> activityRef;
    @NonNull
    private final MyHandler handler = new MyHandler(this);
    //    @NonNull
//    private final SmaliMethodsDialogs popupWindowHelper = new SmaliMethodsDialogs(this);
    private AppCompatTextView filenameTv;
    private View methodMenu;
    private WebView webView;
    private String filePath; // Text file path
    private File htmlFile;

    public HtmlViewDialog(Activity activity) {
        super(activity);
        this.activityRef = new WeakReference<>(activity);
        if (Preferences.getFullScreen()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController insetsController = getWindow().getInsetsController();
                if (insetsController != null) {
                    insetsController.hide(WindowInsets.Type.statusBars());
                }
            } else {
                getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                );
            }
        }
        setContentView(R.layout.dlg_htmlview);
        init();
    }

    @Override
    public void gotoLine(int lineNO) {
        loadHtml(lineNO);
    }

    private void init() {
        this.filenameTv = findViewById(R.id.filename);
        this.methodMenu = findViewById(R.id.menu_methods);
        this.webView = (WebView) findViewById(R.id.webView);
        View editorBtn = findViewById(R.id.editorBtn);

        methodMenu.setOnClickListener(this);
        editorBtn.setOnClickListener(this);
    }

    // lineNO start at 0
    private void loadHtml(int lineNO) {
        if (htmlFile != null) {
            String append = "";
            if (lineNO > 0) {
                append = "#line" + lineNO;
            }
            webView.loadUrl("file://" + htmlFile.getAbsolutePath() + append);
        }
    }

    private void convert2Html(final String filePath) {
        new Thread(() -> {
            boolean ret = false;
            if (TextEditNormalActivity.isXml(filePath)) {
                ret = convertXml2Html(filePath);
            } else if (TextEditNormalActivity.isSmali(filePath)) {
                ret = convertSmali2Html(filePath);
            }
            if (ret) {
                handler.sendEmptyMessage(MyHandler.HTML_LOADED);
            }
        }).start();
    }

    // Convert the file to html format
    private boolean convertXml2Html(String filePath) {
        ArrayList<String> xmlLines = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String data;
            while ((data = br.readLine()) != null) {
                xmlLines.add(data);
                // sb.append(data);
                // sb.append("\n");
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        // Transform to html
        copyCssFile();
        File rootFile = activityRef.get().getFilesDir();
        File htmlFile = new File(rootFile, ".html");
        try {
            if (TextEditNormalActivity.isValuesXml(filePath)) {
                new ValuesXml2Html().transform(xmlLines,
                        htmlFile.getAbsolutePath());
            } else {
                Xml2Html.transform(xmlLines, htmlFile.getAbsolutePath());
            }
            this.htmlFile = htmlFile;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    // Smali -> html
    private boolean convertSmali2Html(String filePath) {
        Smali2Html sh = new Smali2Html(filePath);
        File rootFile = activityRef.get().getFilesDir();
        File htmlFile = new File(rootFile, ".html");
        try {
            sh.transformTo(htmlFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        this.htmlFile = htmlFile;
        return true;
    }

    // If viewsource.css not copied, copy it
    private void copyCssFile() {
        try {
            File rootFile = activityRef.get().getFilesDir();
            File cssFile = new File(rootFile, "viewsource.css");
            if (!cssFile.exists()) {
                InputStream input = activityRef.get().getAssets().open("viewsource.css");
                FileOutputStream fos = new FileOutputStream(cssFile);
                IOUtils.copy(input, fos);
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(@NonNull View view) {
        int id = view.getId();
        if (id == R.id.menu_methods) {
            showPopWindow();
        } else if (id == R.id.editorBtn) {
            dismiss();
        }
    }

    public void show(@NonNull String filePath, String displayName) {
        if (!filePath.equals(this.filePath)) {
            this.filePath = filePath;
            filenameTv.setText(displayName);
            if (TextEditNormalActivity.isSmali(filePath)) {
                methodMenu.setVisibility(View.VISIBLE);
            } else {
                methodMenu.setVisibility(View.GONE);
            }
            convert2Html(filePath);
        }
        show();
    }

    private void showPopWindow() {
        // The popup window is initialized
//        try {
//            String content = new TextFileReader(filePath).getContents();
//            popupWindowHelper.asyncShowPopup(activityRef.get(), filePath, content);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    private static class MyHandler extends Handler {
        private static final int HTML_LOADED = 0;

        private final WeakReference<HtmlViewDialog> dlgRef;

        MyHandler(HtmlViewDialog dlg) {
            dlgRef = new WeakReference<>(dlg);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == HTML_LOADED) {
                if (dlgRef.get() != null) {
                    dlgRef.get().loadHtml(-1);
                }
            }
        }
    }
}
