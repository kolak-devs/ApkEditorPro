package com.mcal.common.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.mcal.common.R
import org.jetbrains.annotations.Contract
import java.io.File

object OpenFiles {
    private fun getHtmlFileIntent(uri: Uri): Intent {
        val intent = Intent("android.intent.action.VIEW")
        intent.setDataAndType(uri, "text/html")
        return intent
    }

    private fun getImageFileIntent(uri: Uri): Intent {
        val intent = Intent("android.intent.action.VIEW")
        intent.addCategory("android.intent.category.DEFAULT")
        intent.setDataAndType(uri, "image/*")
        return intent
    }

    private fun getPdfFileIntent(uri: Uri): Intent {
        val intent = Intent("android.intent.action.VIEW")
        intent.addCategory("android.intent.category.DEFAULT")
        intent.setDataAndType(uri, "application/pdf")
        return intent
    }

    private fun getTextFileIntent(uri: Uri): Intent {
        val intent = Intent("android.intent.action.VIEW")
        intent.addCategory("android.intent.category.DEFAULT")
        intent.setDataAndType(uri, "text/plain")
        return intent
    }

    private fun getAudioFileIntent(uri: Uri): Intent {
        val intent = Intent("android.intent.action.VIEW")
        intent.putExtra("oneshot", 0)
        intent.putExtra("configchange", 0)
        intent.setDataAndType(uri, "audio/*")
        return intent
    }

    private fun getVideoFileIntent(uri: Uri): Intent {
        val intent = Intent("android.intent.action.VIEW")
        intent.putExtra("oneshot", 0)
        intent.putExtra("configchange", 0)
        intent.setDataAndType(uri, "video/*")
        return intent
    }

    private fun getWordFileIntent(uri: Uri): Intent {
        val intent = Intent("android.intent.action.VIEW")
        intent.addCategory("android.intent.category.DEFAULT")
        intent.setDataAndType(uri, "application/msword")
        return intent
    }

    private fun getExcelFileIntent(uri: Uri): Intent {
        val intent = Intent("android.intent.action.VIEW")
        intent.addCategory("android.intent.category.DEFAULT")
        intent.setDataAndType(uri, "application/vnd.ms-excel")
        return intent
    }

    private fun getPPTFileIntent(uri: Uri): Intent {
        val intent = Intent("android.intent.action.VIEW")
        intent.addCategory("android.intent.category.DEFAULT")
        intent.setDataAndType(uri, "application/vnd.ms-powerpoint")
        return intent
    }

    private fun getApkFileIntent(uri: Uri): Intent {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        return intent
    }

    @Contract(pure = true)
    private fun checkEndsWithInStringArray(
        checkItsEnd: String,
        fileTypes: Array<String>
    ): Boolean {
        for (aEnd in fileTypes) {
            if (checkItsEnd.endsWith(aEnd)) return true
        }
        return false
    }

    fun getIntent(ctx: Context, filePath: String): Intent? {
        val intent: Intent
        var uri: Uri? = null
        try {
            uri = FileProvider.getUriForFile(ctx, "com.mcal.apkeditor.pro", File(filePath))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        if (uri == null) {
            return null
        }
        if (checkEndsWithInStringArray(
                filePath, ctx.resources
                    .getStringArray(R.array.fileTypeImage)
            )
        ) {
            intent = getImageFileIntent(uri)
        } else if (checkEndsWithInStringArray(
                filePath, ctx.resources
                    .getStringArray(R.array.fileTypeWebText)
            )
        ) {
            intent = getHtmlFileIntent(uri)
        } else if (checkEndsWithInStringArray(
                filePath, ctx.resources
                    .getStringArray(R.array.fileTypePackage)
            )
        ) {
            intent = getApkFileIntent(uri)
        } else if (checkEndsWithInStringArray(
                filePath, ctx.resources
                    .getStringArray(R.array.fileTypeAudio)
            )
        ) {
            intent = getAudioFileIntent(uri)
        } else if (checkEndsWithInStringArray(
                filePath, ctx.resources
                    .getStringArray(R.array.fileTypeVideo)
            )
        ) {
            intent = getVideoFileIntent(uri)
        } else if (checkEndsWithInStringArray(
                filePath, ctx.resources
                    .getStringArray(R.array.fileTypeText)
            )
        ) {
            intent = getTextFileIntent(uri)
        } else if (checkEndsWithInStringArray(
                filePath, ctx.resources
                    .getStringArray(R.array.fileTypePdf)
            )
        ) {
            intent = getPdfFileIntent(uri)
        } else if (checkEndsWithInStringArray(
                filePath, ctx.resources
                    .getStringArray(R.array.fileTypeWord)
            )
        ) {
            intent = getWordFileIntent(uri)
        } else if (checkEndsWithInStringArray(
                filePath, ctx.resources
                    .getStringArray(R.array.fileTypeExcel)
            )
        ) {
            intent = getExcelFileIntent(uri)
        } else if (checkEndsWithInStringArray(
                filePath, ctx.resources
                    .getStringArray(R.array.fileTypePPT)
            )
        ) {
            intent = getPPTFileIntent(uri)
        } else {
            intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "*/*")
        }
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        return intent
    }

    @JvmStatic
    fun openFile(ctx: Context, filePath: String) {
        val intent = getIntent(ctx, filePath)
        if (intent != null) {
            ctx.startActivity(intent)
        }
    }

    @JvmStatic
    fun openFile(activity: Activity, filePath: String, requestCode: Int) {
        val intent = getIntent(activity, filePath)
        if (intent != null) {
            activity.startActivityForResult(intent, requestCode)
        }
    }
}