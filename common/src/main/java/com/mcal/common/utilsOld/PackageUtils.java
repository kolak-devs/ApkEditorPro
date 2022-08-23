package com.mcal.common.utilsOld;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

public class PackageUtils {
    public static void uninstallPackage(@NonNull Context ctx, String packageName) {
        Uri packageURI = Uri.parse("package:" + packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE,
                packageURI);
        ctx.startActivity(uninstallIntent);
    }
}
