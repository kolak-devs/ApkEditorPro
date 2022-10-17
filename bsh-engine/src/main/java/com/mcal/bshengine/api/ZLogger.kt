package com.mcal.bshengine.api

import android.content.Context
import android.widget.TextView

class ZLogger(context: Context) {
    private var count = 0
    fun info(textView: TextView, i: String) {
        val text = textView.text.toString()
        count++
        if (text.isEmpty()) {
            textView.text = buildString {
                append(count)
                append(": ")
                append(i)
            }
        } else {
            textView.text = buildString {
                append(text)
                append("\n")
                append(count)
                append(": ")
                append(i)
            }
        }
    }
}