package com.gmail.heagoo.apkeditor;

import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebView;

import com.mcal.common.utils.CustomizedLangActivity;

public class EditorHelpActivity extends CustomizedLangActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (GlobalConfig.instance(this).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        this.setContentView(R.layout.activity_help);

        WebView v = this.findViewById(R.id.helpWeb);
        String url = "file:///android_res/raw/editor_help.htm";

        v.loadUrl(url);
    }

}
