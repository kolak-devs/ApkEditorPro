package com.mcal.colorconverter

import android.text.Editable
import android.text.TextWatcher
import com.mcal.colorconverter.ColorPickerConverter.Companion.getWatcher

class WatcherText(private val cp: ColorPickerConverter, private val index: Int) : TextWatcher {
    override fun afterTextChanged(s: Editable) = Unit
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        getWatcher(cp, index, s.toString())
    }
}