package com.mcal.common.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ThumbnailUtils
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