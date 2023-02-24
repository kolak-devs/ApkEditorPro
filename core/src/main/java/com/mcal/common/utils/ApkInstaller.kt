package com.mcal.common.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {
    fun install(ctx: Context, targetApkPath: String) {
        try {
            FileProvider.getUriForFile(ctx, ctx.packageName, File(targetApkPath))?.let { fileUri ->
                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                intent.setDataAndType(fileUri, "application/vnd.android.package-archive")
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ctx.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(ctx, "Internal error: " + e.message, Toast.LENGTH_LONG).show()
        }
    }
}