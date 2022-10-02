package com.mcal.common.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.widget.ImageView
import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min

class ImageHelper {
    private var originWidth = 0
    private var originHeight = 0

    fun getImageThumbnail(imagePath: String, width: Int, height: Int): Bitmap? {
        var bitmap: Bitmap?
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        bitmap = BitmapFactory.decodeFile(imagePath, options)
        options.inJustDecodeBounds = false
        originHeight = options.outHeight
        originWidth = options.outWidth
        val beWidth = originWidth / width
        val beHeight = originHeight / height
        var be = min(beWidth, beHeight)
        if (be <= 0) {
            be = 1
        }
        options.inSampleSize = be
        bitmap = BitmapFactory.decodeFile(imagePath, options)
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT)
        return bitmap
    }

    fun getImagePreview(drawable: Drawable, width: Int, height: Int): Bitmap? {
        return ThumbnailUtils.extractThumbnail(drawable.toBitmap(), width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT)
    }

    fun getOriginWidth(): Int {
        return originWidth
    }

    fun getOriginHeight(): Int {
        return originHeight
    }

    // Zoom to target width and height
    fun zoomImage(bitmap: Bitmap, width: Int, height: Int, outFilePath: String) {
        val bmpWidth = bitmap.width
        val bmpHeight = bitmap.height

        // Zoom ratio
        val scaleWidth = width.toFloat() / bmpWidth
        val scaleHeight = height.toFloat() / bmpHeight
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        val resizeBitmap = Bitmap.createBitmap(bitmap, 0, 0, bmpWidth, bmpHeight, matrix, false)

        // Save to file
        var os: FileOutputStream? = null
        try {
            os = FileOutputStream(outFilePath)
            if (outFilePath.endsWith(".png")) {
                resizeBitmap.compress(Bitmap.CompressFormat.PNG, 80, os)
            } else {
                resizeBitmap.compress(Bitmap.CompressFormat.JPEG, 80, os)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (os != null) {
                try {
                    os.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}

fun Drawable.toBitmap(): Bitmap? {
    if (this is BitmapDrawable) {
        if (this.bitmap != null) {
            return this.bitmap
        }
    }
    val bitmap: Bitmap = if (this.intrinsicWidth <= 0 || this.intrinsicHeight <= 0) {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
    } else {
        Bitmap.createBitmap(this.intrinsicWidth, this.intrinsicHeight, Bitmap.Config.ARGB_8888)
    }
    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)
    return bitmap
}

fun ImageView.imageLoader(icon: Int) {
    CoroutineScope(Dispatchers.Main).launch {
        this@imageLoader.setImageResource(icon)
    }
}

fun ImageView.imageLoader(icon: Drawable) {
    CoroutineScope(Dispatchers.IO).launch {
        val async = async {
            BitmapDrawable(resources, ImageHelper().getImagePreview(icon, 200, 200))
        }
        val result = async.await()
        withContext(Dispatchers.Main) {
            this@imageLoader.setImageDrawable(result)
        }
    }
}