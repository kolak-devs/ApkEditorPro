package com.mcal.bshengine.api

import android.widget.TextView

class XLog(val textView: TextView) {
    private var count = 0
    fun info(message: String) {
        val text = textView.text.toString()
        count++
        if (text.isEmpty()) {
            textView.text = buildString {
                append(count)
                append(": ")
                append(message)
            }
        } else {
            textView.text = buildString {
                append(text)
                append("\n")
                append(count)
                append(": ")
                append(message)
            }
        }
    }

    fun clear() {
        val view = textView
        if (view.text.isNotEmpty()) {
            view.text = ""
        }
    }
}