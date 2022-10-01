package com.mcal.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

object BitmapHelper {
    @JvmStatic
    fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap {
        return when (val drawable = AppCompatResources.getDrawable(context, drawableId)) {
            is BitmapDrawable -> {
                drawable.bitmap
            }
            is VectorDrawableCompat, is VectorDrawable -> {
                val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
            else -> {
                throw IllegalArgumentException("unsupported drawable type")
            }
        }
    }
}