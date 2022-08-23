package com.mcal.patchview.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.annotation.NonNull;

/**
 * Helper class used for decor related functions
 */
public class ResourceHelper {

    /**
     * Utility method to convert from dp to pixels
     *
     * @param context context to get resources
     * @param dp      to convert
     * @return value in px
     */
    public static int dpToPx(@NonNull Context context, int dp) {
        Resources r = context.getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }
}