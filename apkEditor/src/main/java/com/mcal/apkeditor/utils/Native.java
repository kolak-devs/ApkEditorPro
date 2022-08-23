package com.mcal.apkeditor.utils;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

public class Native {
    static {
        System.loadLibrary("apkeditorpro");
    }

    private static Native instance;

    private static Native getInstance() {
        if (instance == null) {
            instance = new Native();
        }
        return instance;
    }

    public static String getSignature(@NonNull Activity activity) {
        return getInstance().getSignature(activity.getApplicationContext());
    }

    public static String getSignatureSalted(@NonNull Activity activity, String saltStr) {
        return getInstance().getSignatureSalted(activity.getApplicationContext(), saltStr);
    }

    public native String getSignature(Context context);

    public native String getSignatureSalted(Context context, String saltStr);
}
