package com.mcal.common.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.util.zip.ZipFile
import kotlin.math.min

class ZipImageHelper(private val file: ZipFile) {
    var originWidth = 0
        private set
    var originHeight = 0
        private set

    fun getImageThumbnail(entryName: String?, width: Int, height: Int): Bitmap? {
        var input: InputStream? = null
        try {
            val entry = file.getEntry(entryName)
            input = file.getInputStream(entry)
            var bitmap = BitmapFactory.decodeStream(input)
            originWidth = bitmap.width
            originHeight = bitmap.height
            val beWidth = originWidth / width
            val beHeight = originHeight / height
            var be = min(beWidth, beHeight)
            if (be <= 0) {
                be = 1
            }
            if (be > 1) {
                bitmap = ThumbnailUtils.extractThumbnail(
                    bitmap, width, height,
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT
                )
            }
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }
}