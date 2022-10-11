package com.mcal.common.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardUtils {
    @JvmStatic
    fun copyToClipboard(ctx: Context, str: String?) {
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", str)
        clipboard.setPrimaryClip(clip)
    }

    @JvmStatic
    fun getClipboardText(ctx: Context): String? {
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            return clip.getItemAt(0).coerceToText(ctx).toString()
        }
        return null
    }
}