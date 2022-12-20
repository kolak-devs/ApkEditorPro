package com.mcal.pngeditor

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.davemorrissey.labs.subscaleview.ImageSource
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.pngeditor.databinding.ActivityPhotoViewBinding

class PhotoViewerActivity : CustomizedLangActivity() {
    private var filepath: String? = null
    private lateinit var binding: ActivityPhotoViewBinding
    private var colorSelected = -1

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.extras != null) {
            filepath = intent.getStringExtra("filePath")

            filepath?.let { path ->

                binding.image.setImage(ImageSource.uri(path))

                val message = "(" + binding.image.sWidth + "x" + binding.image.sHeight + ")"
                setupToolbar(R.id.toolbar, getString(R.string.app_name), message, true)
            }
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
}