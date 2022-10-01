package com.mcal.common.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;

public class ImageZoomer {
    private int originWidth;
    private int originHeight;

    public Bitmap getImageThumbnail(String imagePath, int width, int height) {
        Bitmap bitmap;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        options.inJustDecodeBounds = false;
        originHeight = options.outHeight;
        originWidth = options.outWidth;
        int beWidth = originWidth / width;
        int beHeight = originHeight / height;
        int be = Math.min(beWidth, beHeight);
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    public int getOriginWidth() {
        return originWidth;
    }

    public int getOriginHeight() {
        return originHeight;
    }
}
