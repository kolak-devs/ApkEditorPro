package com.mcal.apkeditor.util;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;

import androidx.annotation.NonNull;

public class Test {
    public String test(@NonNull Context ctx) {
        ContentResolver r = ctx.getContentResolver();
        @SuppressLint("HardwareIds") String id = android.provider.Settings.Secure.getString(r, "android_id");
        return Utils.stringAdd1(id);
    }

    public void testStack() {
        Utils.printCallStack();
    }
}
