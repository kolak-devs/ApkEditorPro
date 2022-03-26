package com.mcal.apkeditor.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.annotation.NonNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Image {
    public Bitmap bitmap;

    public Image(@NonNull Context context, int id) {
        bitmap = BitmapFactory.decodeResource(context.getResources(), id);
    }

    public Image(Context context, String name, float scaleW, float scaleH) throws IOException {
        //InputStream is = context.getAssets().open(name);
        InputStream is = new FileInputStream(name);
        bitmap = BitmapFactory.decodeStream(is);
        is.close();
        //bitmap = setScaleSize(bitmap, scaleW,scaleH);
    }

    public static void free(Image image) {
        try {
            if (image.bitmap != null) {
                if (!image.bitmap.isRecycled()) {//如果没有回收
                    //回收图片所占的内存
                    image.bitmap.recycle();
                    if (image.bitmap.isRecycled()) {
                        image.bitmap = null;
                    }
                } else {
                    image.bitmap = null;
                }
            } else {
                image.bitmap = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getWidth() {
        return bitmap.getWidth();
    }

    public int getHeight() {
        return bitmap.getHeight();
    }

    public int[] getPixel(int[] array, int x, int y) {
        bitmap.getPixels(array, 0, getWidth(), 0, 0, getWidth(), getHeight());
        return array;
    }

    public Bitmap CreateImage(int[] array, int w, int h) {
        return Bitmap.createBitmap(array, w, h, Bitmap.Config.ARGB_4444);
    }

    private Bitmap setScaleSize(Bitmap bitmap, float arg0, float arg1) {
        Matrix matrix = new Matrix();
        matrix.postScale(arg0, arg1);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, getWidth(), getHeight(), matrix, true);
        return bitmap;
    }
}