package com.mcal.bshengine.api

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class XToast(private val context: Context) {
    fun show(message: String, durationLong: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, if (durationLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }
}