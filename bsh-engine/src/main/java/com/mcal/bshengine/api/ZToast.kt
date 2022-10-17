package com.mcal.bshengine.api

import android.content.Context
import android.widget.Toast

class ZToast(private val context: Context) {
    fun show(message: String, isLong: Boolean) {
        Toast.makeText(context, message, if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}