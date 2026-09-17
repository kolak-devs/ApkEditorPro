package com.mcal.common.utils

import android.content.Context
import android.graphics.drawable.Drawable

class ApkInfoParser {
    @Throws(Exception::class)
    fun parse(ctx: Context, apkPath: String?): AppInfo? {
        var apkInfo: AppInfo? = null
        val packageManager = ctx.packageManager
        apkPath?.let { path ->
            packageManager.getPackageArchiveInfo(path, 0)?.let { packageInfo ->
                val appInfo = packageInfo.applicationInfo ?: return@let
                appInfo.sourceDir = path
                appInfo.publicSourceDir = path
                apkInfo = AppInfo().apply {
                    label = appInfo.loadLabel(packageManager).toString()
                    pkgName = packageInfo.packageName
                    icon = appInfo.loadIcon(packageManager)
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