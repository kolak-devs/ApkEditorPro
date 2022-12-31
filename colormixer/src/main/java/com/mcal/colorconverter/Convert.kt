package com.mcal.colorconverter

import android.graphics.Color

object Convert {
    fun fromArgbToSmali(text: String): String {
        var input = text
        val result: String
        if (input.startsWith("#")) {
            input = input.substring(1)
        }
        if (input.length == 3 || input.length == 4) {
            var twoChars = ""
            for (i in input.indices) {
                twoChars += input[i]
                twoChars += input[i]
            }
            input = twoChars
        }
        if (input.length == 6) {
            input = "ff$input"
        }
        val color = Color.parseColor("#$input")
        val decAlpha = input.substring(0, 2).toInt(16)
        result = if (decAlpha >= 128) {
            "-0x" + Integer.toHexString(color * -1)
        } else {
            "0x" + Integer.toHexString(color)
        }
        return result
    }

    fun fromArgbToJava(text: String): String {
        var input = text
        if (input.startsWith("#")) {
            input = input.substring(1)
        }
        if (input.length == 3 || input.length == 4) {
            var twoChars = ""
            for (i in input.indices) {
                twoChars += input[i]
                twoChars += input[i]
            }
            input = twoChars
        }
        if (input.length == 6) {
            input = "ff$input"
        }
        return Color.parseColor("#$input").toString()
    }

    fun fromSmaliToArgb(text: String?): String {
        var result = ""
        if (text != null && text.isNotEmpty() && text.length > 2 && text.contains("0x")) {
            val decodedInteger = Integer.decode(text)
            var a = Integer.toHexString(Color.alpha(decodedInteger))
            var r = Integer.toHexString(Color.red(decodedInteger))
            var g = Integer.toHexString(Color.green(decodedInteger))
            var b = Integer.toHexString(Color.blue(decodedInteger))
            if (a.length == 1) a = "0$a"
            if (r.length == 1) r = "0$r"
            if (g.length == 1) g = "0$g"
            if (b.length == 1) b = "0$b"
            result = a + r + g + b
        }
        return result
    }

    fun fromSmaliToJava(text: String?): String {
        val resultArgb = fromSmaliToArgb(text)
        return fromArgbToJava(resultArgb)
    }

    fun fromJavaToArgb(text: String): String {
        val decodedInteger = Integer.decode(text)
        var a = Integer.toHexString(Color.alpha(decodedInteger))
        var r = Integer.toHexString(Color.red(decodedInteger))
        var g = Integer.toHexString(Color.green(decodedInteger))
        var b = Integer.toHexString(Color.blue(decodedInteger))
        if (a.length == 1) a = "0$a"
        if (r.length == 1) r = "0$r"
        if (g.length == 1) g = "0$g"
        if (b.length == 1) b = "0$b"
        return a + r + g + b
    }

    fun fromJavaToSmali(text: String): String {
        val resultArgb = fromJavaToArgb(text)
        return fromArgbToSmali(resultArgb)
    }
}