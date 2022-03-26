package com.mcal.common.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ClipboardUtils {

    public static void copyToClipboard(@NonNull Context ctx, String str) {
        // Copy to clipboard
        ClipboardManager clipboard = (ClipboardManager) ctx
                .getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", str);
        clipboard.setPrimaryClip(clip);
    }

    @Nullable
    public static String getText(@NonNull Context ctx) {
        ClipboardManager clipboard = (ClipboardManager) ctx
                .getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            return clip.getItemAt(0).coerceToText(ctx).toString();
        }
        return null;
    }
}
