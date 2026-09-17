package com.mcal.common.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object PackageHelper {
    @JvmStatic
    fun uninstallIntent(ctx: Context, packageName: String): Intent {
        val packageURI = Uri.parse("package:$packageName")
        return Intent(
            Intent.ACTION_DELETE,
            packageURI
        )
    }

    @JvmStatic
    fun uninstallPackage(ctx: Context, packageName: String) {
        ctx.startActivity(uninstallIntent(ctx, packageName))
    }
}