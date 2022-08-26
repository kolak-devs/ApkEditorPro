package com.mcal.common.utils

import android.content.Context
import android.graphics.drawable.Drawable

class ApkInfoParser {
    @Throws(Exception::class)
    fun parse(ctx: Context, apkPath: String?): AppInfo? {
        var apkInfo: AppInfo? = null
        val packageManager = ctx.packageManager
        apkPath?.let { path ->
            packageManager.getPackageArchiveInfo(path, 0)?.apply {
                applicationInfo.sourceDir = path
                applicationInfo.publicSourceDir = path
                apkInfo = AppInfo().apply {
                    label = applicationInfo.loadLabel(packageManager).toString()
                    pkgName = packageName
                    icon = applicationInfo.loadIcon(packageManager)
                }
            }
        }
        return apkInfo
    }

    class AppInfo {
        @JvmField
        var label: String? = null

        @JvmField
        var pkgName: String? = null

        @JvmField
        var icon: Drawable? = null
    }
}