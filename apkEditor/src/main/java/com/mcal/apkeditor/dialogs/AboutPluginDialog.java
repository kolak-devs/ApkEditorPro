package com.mcal.apkeditor.dialogs;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.view.ViewDialog;

public class AboutPluginDialog {

    public AboutPluginDialog(Activity activity) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dlg_about_translate_plugin, null);

        WebView webView = (WebView) view.findViewById(R.id.web_instructions);
        webView.loadUrl("file:///android_res/raw/about_translate_plugin.htm");

        ViewDialog dialog = new ViewDialog(activity);
        dialog.setTitle(R.string.translate_plugin);
        dialog.setView(view);
        dialog.setPositive("Ok", null);
        dialog.show();
    }
}