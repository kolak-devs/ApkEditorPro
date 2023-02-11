package com.mcal.bshengine.api

import android.content.Context
import android.widget.Toast

class XToast(private val context: Context) {
    fun show(message: String, durationLong: Boolean) {
        Toast.makeText(context, message, if (durationLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}