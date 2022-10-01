package com.mcal.common.utils;

import android.app.ActivityManager;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

public class ServiceUtil {
    public static boolean isMyServiceRunning(@NonNull Context ctx, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo service : services) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
