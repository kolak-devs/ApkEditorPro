package com.mcal.pngeditor;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

public interface ImageEditor {
    void setParam(String name, Object value);
    Bitmap edit(Bitmap bitmap);
    boolean isModified();
}
