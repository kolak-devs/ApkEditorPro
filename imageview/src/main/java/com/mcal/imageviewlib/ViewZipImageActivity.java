package com.mcal.imageviewlib;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.ViewGroup;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.mcal.common.activities.CustomizedLangActivity;
import com.mcal.common.utilsOld.ActivityUtils;
import com.polites.android.GestureImageView;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ViewZipImageActivity extends CustomizedLangActivity {
    protected GestureImageView view;

    private String imageFilePath;
    private String zipFilePath;
    private String entryName;

    private int screenWidth;
    private int screenHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) {
            this.finish();
            return;
        }

        setContentView(R.layout.imageviewlib_activity_empty);

        this.zipFilePath = ActivityUtils.getParam(intent, "zipFilePath");
        this.entryName = ActivityUtils.getParam(intent, "entryName");
        this.imageFilePath = ActivityUtils.getParam(intent, "imageFilePath");

        // Decode bitmap
        Bitmap bitmap = decodeBitmap();
        if (bitmap == null) {
            finish();
            return;
        }

        // Get screen window size
        getScreenSize();

        LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT);
        view = new GestureImageView(this, bitmap);
        //view.setImageBitmap(bitmap);
        view.setLayoutParams(params);
        // Small image
        if (bitmap.getWidth() <= screenWidth
                && bitmap.getHeight() <= screenHeight) {
            //view.setStrict(true);
            view.setStartingScale(1.0f);
            view.setStartingPosition(screenWidth / 2.0f,
                    screenHeight / 2.0f);
        }

        ViewGroup layout = (ViewGroup) findViewById(R.id.layout);

        layout.addView(view);
    }

    private void getScreenSize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();

        try {
            display.getSize(size);
        } catch (java.lang.NoSuchMethodError ignore) { // Older device
            size.x = display.getWidth();
            size.y = display.getHeight();
        }

        this.screenWidth = size.x;
        this.screenHeight = size.y;
    }

    private Bitmap decodeBitmap() {
        if (imageFilePath != null) {
            return BitmapFactory.decodeFile(imageFilePath);
        } else {
            return decodeBitmapFromZip();
        }
    }

    @Nullable
    private Bitmap decodeBitmapFromZip() {
        ZipFile zfile = null;
        ZipEntry entry = null;
        InputStream input = null;
        try {
            zfile = new ZipFile(zipFilePath);
            entry = zfile.getEntry(entryName);
            input = zfile.getInputStream(entry);

            Bitmap bitmap = null;
            bitmap = BitmapFactory.decodeStream(input);

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (zfile != null) {
                try {
                    zfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
