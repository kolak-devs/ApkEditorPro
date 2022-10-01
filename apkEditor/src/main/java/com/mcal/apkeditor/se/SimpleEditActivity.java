package com.mcal.apkeditor.se;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

import com.mcal.apkeditor.R;
import com.mcal.apklib.AXMLParser.IReferenceDecode;
import com.mcal.common.activities.CustomizedLangActivity;
import com.mcal.common.utils.ApkInfoParser;
import com.mcal.common.utils.ApkInfoParser.AppInfo;
import com.mcal.common.utils.ActivityHelper;
import com.mcal.common.view.DynamicExpandListView;

import org.jetbrains.annotations.Contract;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleEditActivity extends CustomizedLangActivity implements OnClickListener,
        IReferenceDecode, IDirChanged {
    List<View> views;
    private String apkPath;
    private AppInfo apkInfo;
    private ZipFileListAdapter filesAdapter;
    private ImageListAdapter imagesAdapter;
    private AudioListAdapter audiosAdapter;

    // Thread & handler
    private MyHandler handler;
    private MyThread thread;

    // 3 listview & other header/bottom view
    private ListView fileListView;
    private DynamicExpandListView imageListView;
    private ListView audioListView;
    private LinearLayout centerLayout;
    private int currIndex = 0;
    private int screenWidth;
    private ImageView cursorImage;
    private ViewPager viewPager;
    private View fileLayout;
    private View imageLayout;
    private View audioLayout;
    private TextView fileTitle;
    private TextView imageTitle;
    private TextView audioTitle;
    // Save/Close Button
    private Button closeSaveBtn;
    // Summary text (to show tip)
    private TextView summaryTv;
    // Modified or not
    private boolean isModified = false;
    // To parse all the information inside the APK
    private ZipHelper zipHelper;

    @NonNull
    @Contract(pure = true)
    private static String getPackage(int id) {
        if (id >>> 24 == 1) {
            return "android:";
        }
        return "";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_simpleedit);

        this.apkPath = ActivityHelper.getParam(getIntent(), "apkPath");

        try {
            this.apkInfo = new ApkInfoParser().parse(this, apkPath);
        } catch (Exception e) {
            String msg = getResources().getString(R.string.cannot_parse_apk);
            msg += ": " + e.getMessage();
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }

        if (apkInfo != null) {
            // Start thread to parse the apk file
            this.handler = new MyHandler(this);
            this.thread = new MyThread(this);
            thread.start();

            InitCursorImage();
            initViews();
            InitViewPager();
        } else {
            this.finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if (filesAdapter != null) {
            filesAdapter.destroy();
        }
        if (audiosAdapter != null) {
            audiosAdapter.destroy();
        }
        // imagesAdapter.destroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            // APK successfully modified and installed
            if (resultCode == 1000) {
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save APK path
        outState.putString("apkPath", this.apkPath);
        super.onSaveInstanceState(outState);
    }

    @SuppressLint("InflateParams")
    private void InitViewPager() {
        this.viewPager = (ViewPager) findViewById(R.id.pagerView);
        this.views = new ArrayList<>();

        LayoutInflater inflater = getLayoutInflater();
        this.fileLayout = inflater.inflate(R.layout.pageitem_files, null);
        this.imageLayout = inflater.inflate(R.layout.pageitem_images, null);
        this.audioLayout = inflater.inflate(R.layout.pageitem_audios, null);

        views.add(fileLayout);
        views.add(imageLayout);
        views.add(audioLayout);
        viewPager.setAdapter(new SimpleEditAdapter(views));
        viewPager.setCurrentItem(0);
        viewPager.setOnPageChangeListener(new MyOnPageChangeListener());
    }

    private void InitCursorImage() {
        this.cursorImage = (ImageView) findViewById(R.id.cursor);
        int bmpW = BitmapFactory.decodeResource(getResources(), R.drawable.pager_focus).getWidth();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        this.screenWidth = dm.widthPixels;
        int offset = (screenWidth / 3 - bmpW) / 2;
        Matrix matrix = new Matrix();
        matrix.postTranslate(offset, 0);
        cursorImage.setImageMatrix(matrix);
    }

    public void dataReady(boolean succeed) {
        // Make progress bar gone
        this.findViewById(R.id.progress_bar).setVisibility(View.GONE);

        if (succeed) {
            centerLayout.setVisibility(View.VISIBLE);
            initCenterView();
        } else {
            Toast.makeText(this, thread.getErrorMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initCenterView() {
        // Get View
        this.fileListView = (ListView) fileLayout.findViewById(R.id.files_list);
        this.imageListView = (DynamicExpandListView) imageLayout
                .findViewById(R.id.images_list);
        this.audioListView = (ListView) audioLayout
                .findViewById(R.id.audios_list);

        // Files
        this.filesAdapter = new ZipFileListAdapter(this, this, zipHelper);
        fileListView.setAdapter(filesAdapter);
        fileListView.setOnItemClickListener(filesAdapter);
        fileListView.setOnItemLongClickListener(filesAdapter);

        // Image
        this.imagesAdapter = new ImageListAdapter(imageListView, this,
                zipHelper);
        imageListView.setAdapter(imagesAdapter);
        imageListView.setOnItemClickListener(imagesAdapter);
        imageListView.setOnItemLongClickListener(imagesAdapter);

        // Audio
        audiosAdapter = new AudioListAdapter(this, zipHelper);
        audioListView.setAdapter(audiosAdapter);
        audioListView.setOnItemClickListener(audiosAdapter);
        audioListView.setOnItemLongClickListener(audiosAdapter);
    }

    private void initViews() {
        this.centerLayout = (LinearLayout) this
                .findViewById(R.id.center_layout);
        this.summaryTv = (TextView) this.findViewById(R.id.tv_summary);
        this.fileTitle = (TextView) this.findViewById(R.id.files_label);
        this.imageTitle = (TextView) this.findViewById(R.id.images_label);
        this.audioTitle = (TextView) this.findViewById(R.id.audio_label);
        this.closeSaveBtn = (Button) this.findViewById(R.id.btn_close);

        // Set center content invisible
        centerLayout.setVisibility(View.INVISIBLE);

        this.fileTitle.setOnClickListener(this);
        this.imageTitle.setOnClickListener(this);
        this.audioTitle.setOnClickListener(this);
        this.closeSaveBtn.setOnClickListener(this);

        // Basic info
        if (apkInfo != null) {
            ImageView apkIcon = (ImageView) this.findViewById(R.id.apk_icon);
            apkIcon.setImageDrawable(apkInfo.icon);

            TextView labelTV = (TextView) this.findViewById(R.id.apk_label);
            labelTV.setText(apkInfo.label);
        }
    }

    private void centerViewChanged() {
        switch (this.currIndex) {
            case 0: {
                String strDir = filesAdapter.getCurrentDir();
                summaryTv.setText(strDir);
            }
            break;
            case 1:
                if (zipHelper != null) {
                    int num = zipHelper.getImageNum();
                    String str = (String) getResources().getText(
                            R.string.image_summary);
                    String msg = String.format(str, num);
                    summaryTv.setText(msg);
                }
                break;
            case 2:
                if (zipHelper != null) {
                    int num = zipHelper.getAudioNum();
                    String str = (String) getResources().getText(
                            R.string.audio_summary);
                    String msg = String.format(str, num);
                    summaryTv.setText(msg);
                }
                break;
        }
    }

    private void initData() throws Exception {
        zipHelper = new ZipHelper(this.apkPath);
        zipHelper.parse();
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();
        if (id == R.id.files_label) {
            this.currIndex = 0;
            viewPager.setCurrentItem(currIndex);
        } else if (id == R.id.audio_label) {
            this.currIndex = 2;
            viewPager.setCurrentItem(currIndex);
        } else if (id == R.id.images_label) {
            this.currIndex = 1;
            viewPager.setCurrentItem(currIndex);
        } else if (id == R.id.btn_close) {
            if (this.isModified) {
                makeAPK();
                this.finish();
            } else {
                this.finish();
            }
        }
    }

    // To make the new modified APK
    private void makeAPK() {
        Map<String, String> imgReplaces = imagesAdapter.getReplaces();
        Map<String, String> fileReplaces = filesAdapter.getReplaces();
        Map<String, String> audioReplaces = audiosAdapter.getReplaces();

        Intent intent = new Intent(this, ApkCreateActivity.class);
        ActivityHelper.attachParam(intent, "apkPath", this.apkPath);
        ActivityHelper.attachParam(intent, "packageName", apkInfo.pkgName);
        ActivityHelper.attachParam(intent, "imageReplaces", imgReplaces);
        if (!fileReplaces.isEmpty() || !audioReplaces.isEmpty()) {
            fileReplaces.putAll(audioReplaces);
            ActivityHelper.attachParam(intent, "otherReplaces", fileReplaces);
        }

        startActivity(intent);
    }

    @Override
    public String getResReference(int data) {
        return String.format("@%s%08X", getPackage(data), data);
    }

    @Override
    public void dirChanged(String dir) {
        this.summaryTv.setText(dir);
    }

    public void setModified() {
        if (!this.isModified) {
            this.closeSaveBtn.setText(R.string.save);
            this.isModified = true;
        }
    }

    private static class MyHandler extends Handler {
        WeakReference<SimpleEditActivity> activityRef;

        public MyHandler(SimpleEditActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SimpleEditActivity activity = activityRef.get();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                case 0:
                    activity.dataReady(true);
                    break;
                case 1:
                    activity.dataReady(false);
                    break;
            }
        }
    }

    private static class MyThread extends Thread {
        String err;
        WeakReference<SimpleEditActivity> activityRef;

        public MyThread(SimpleEditActivity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            SimpleEditActivity activity = activityRef.get();
            if (activity != null) {
                try {
                    activity.initData();
                    activity.handler.sendEmptyMessage(0);
                } catch (Exception e) {
                    e.printStackTrace();
                    err = e.getMessage();
                    activity.handler.sendEmptyMessage(1);
                }
            }
        }

        public String getErrorMessage() {
            return err;
        }
    }

    public class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {

        int one = screenWidth / 3;
        int two = one * 2;

        public void onPageScrollStateChanged(int arg0) {

        }

        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        public void onPageSelected(int position) {
            Animation animation = new TranslateAnimation(one * currIndex, one
                    * position, 0, 0);
            currIndex = position;
            animation.setFillAfter(true);
            animation.setDuration(200);
            cursorImage.startAnimation(animation);

            // Notify to change cursor/banner
            centerViewChanged();
        }
    }
}
