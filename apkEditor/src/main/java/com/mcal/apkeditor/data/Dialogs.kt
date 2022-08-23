package com.mcal.apkeditor.data

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.BuildConfig
import com.mcal.apkeditor.R

object Dialogs {
    @SuppressLint("SetTextI18n")
    fun about(context: Context) {
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val ll = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 0, 40, 0)
            layoutParams = params
        }
        val msg = AppCompatTextView(context)
        try {
            val pInfo = context.packageManager
                .getPackageInfo(context.packageName, 0)
            msg.text = "" +
                    "Version: ${pInfo.versionName}\n\n" +
                    "Smali: ${BuildConfig.SMALI_VERSION}\n" +
                    "JaDX: ${BuildConfig.JADX_VERSION}\n" +
                    "ApkTool: ${BuildConfig.APKTOOL_VERSION}\n" +
                    "Android API: v32\n\n" +
                    "Created by Russian with ❤️\n" +
                    "© Copyright 2021-2022 Тимашков Иван"
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ll.addView(msg)
        val dialog = MaterialAlertDialogBuilder(context)
        dialog.setTitle(R.string.app_name)
        dialog.setView(ll)
        dialog.setPositiveButton("OK", null)
        dialog.show()
    }
}