package com.mcal.common.utils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;

import androidx.annotation.NonNull;

import java.io.FileOutputStream;
import java.io.IOException;

public class ImageTool {
    // Zoom to target width and height
    public void zoomImage(@NonNull Bitmap bitmap, int width, int height, String outFilePath) {
        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();

        // Zoom ratio
        float scaleWidth = (float) width / bmpWidth;
        float scaleHeight = (float) height / bmpHeight;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizeBitmap = Bitmap.createBitmap(bitmap, 0, 0, bmpWidth, bmpHeight, matrix, false);

        // Save to file
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outFilePath);
            if (outFilePath.endsWith(".png")) {
                resizeBitmap.compress(CompressFormat.PNG, 80, os);
            } else {
                resizeBitmap.compress(CompressFormat.JPEG, 80, os);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}