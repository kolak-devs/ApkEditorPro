package com.mcal.pngeditor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.pngeditor.databinding.ActivityPhotoViewBinding
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ViewZipImageActivity : CustomizedLangActivity() {
    private var imageFilePath: String? = null
    private var zipFilePath: String? = null
    private var entryName: String? = null
    private var colorSelected = -1
    private lateinit var binding: ActivityPhotoViewBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intent = intent
        if (intent.extras != null) {
            zipFilePath = intent.getStringExtra("zipFilePath")
            entryName = intent.getStringExtra("entryName")
            imageFilePath = intent.getStringExtra("imageFilePath")
            decodeBitmap()?.let { bitmap ->
                binding.image.setImageBitmap(bitmap)
                val message = "(" + bitmap.width + "x" + bitmap.height + ")"
                setupToolbar(R.id.toolbar, "ApkEditor", message, true)
            }
        }
    }

    private fun decodeBitmap(): Bitmap? {
        return if (imageFilePath != null) {
            BitmapFactory.decodeFile(imageFilePath)
        } else {
            decodeBitmapFromZip()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_photo_viewer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_background -> {
                colorSelected++
                val color = when (colorSelected) {
                    0 -> {
                        Color.BLACK
                    }
                    1 -> {
                        Color.WHITE
                    }
                    else -> {
                        colorSelected = -1
                        Color.GRAY
                    }
                }
                binding.image.setBackgroundColor(color)
            }
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun decodeBitmapFromZip(): Bitmap? {
        var zfile: ZipFile? = null
        val entry: ZipEntry
        var input: InputStream? = null
        try {
            zfile = ZipFile(zipFilePath)
            entry = zfile.getEntry(entryName)
            input = zfile.getInputStream(entry)
            return BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (zfile != null) {
                try {
                    zfile.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }
}