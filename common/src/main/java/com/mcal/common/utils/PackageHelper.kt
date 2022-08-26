package com.mcal.common.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object PackageHelper {
    @JvmStatic
    fun uninstallPackage(ctx: Context, packageName: String) {
        val packageURI = Uri.parse("package:$packageName")
        val uninstallIntent = Intent(
            Intent.ACTION_DELETE,
            packageURI
        )
        ctx.startActivity(uninstallIntent)
    }
}