package com.mcal.colorconverter

import android.text.InputFilter
import android.text.Spanned

class FilterInput(private val regex: String) : InputFilter {
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence {
        return if (source.toString().matches(Regex(regex))) source else ""
    }
}