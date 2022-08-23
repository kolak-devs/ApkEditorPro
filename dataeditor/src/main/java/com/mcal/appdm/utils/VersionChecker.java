package com.mcal.appdm.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;

import androidx.annotation.NonNull;

public class VersionChecker {

    public static boolean isProVersion(@NonNull Context ctx) {
        ApplicationInfo ai = ctx.getApplicationInfo();
        return ai.packageName.endsWith(".pro");
    }

    public static boolean isFreeVersion(Context ctx) {
        return !isProVersion(ctx);
    }

    // To view/download the pro version
    public static void viewProVersion(@NonNull Context ctx) {
        String pkgName = ctx.getPackageName() + ".pro";
        Uri uri = Uri.parse("market://details?id=" + pkgName);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            ctx.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse("http://play.google.com/store/apps/details?id="
                            + pkgName)));
        }
    }
}
