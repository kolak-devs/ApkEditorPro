package com.mcal.apkeditor.dialogs;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;

import androidx.appcompat.widget.Toolbar;

import com.mcal.apkeditor.R;
import com.mcal.apkeditor.view.ViewDialog;

public class AboutPluginDialog {

    public AboutPluginDialog(Activity activity) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        LinearLayout ll = new LinearLayout(activity);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(0, 0, 0, 0);
        ll.setLayoutParams(layoutParams);

        WebView webView = new WebView(activity);
        webView.loadUrl("file:///android_res/raw/about_translate_plugin.html");

        ll.addView(webView);

        ViewDialog dialog = new ViewDialog(activity);
        dialog.setTitle(R.string.translate_plugin);
        dialog.setView(ll);
        dialog.setPositive("Ok", null);
        dialog.show();
    }
}