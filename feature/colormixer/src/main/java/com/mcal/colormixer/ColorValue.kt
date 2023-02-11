package com.mcal.colormixer

import android.annotation.SuppressLint
import android.content.Context
import com.mcal.common.utils.ReflectionHelper

class ColorValue(
    @JvmField
    var name: String,
    @JvmField
    var strColorValue: String
) {
    @JvmField
    var intColorValue = 0

    // intValue successfully parsed from strValue, or not
    @JvmField
    var parsed = false

    init {
        parseColorFrom(strColorValue)
    }

    // str = "#ffffffff"
    // str = "@color/xyz"
    // str = "@android:color/xyz"
    private fun parseColorFrom(str: String) {
        if (str.startsWith("#")) {
            try {
                intColorValue = str.substring(1).toLong(16).toInt()
                parsed = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Parse the reference color
    @SuppressLint("NewApi")
    fun parseRefColor(ctx: Context, values: List<ColorValue>) {
        if (!parsed) {
            if (strColorValue.startsWith("@color/")) {
                val refStr = strColorValue.substring(7)
                for (i in values.indices) {
                    val v = values[i]
                    if (v.parsed && refStr == v.name) {
                        intColorValue = v.intColorValue
                        parsed = true
                        break
                    }
                }
            } else if (strColorValue.startsWith("@android:color/")) {
                try {
                    val refStr = strColorValue.substring(15)
                    ReflectionHelper.getStaticFieldObject("android.R\$color", refStr)?.let { obj ->
                        intColorValue = ctx.getColor((obj as Int))
                        parsed = true
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun toString(): String {
        return "    <color name=\"" + name + "\">" +
                strColorValue +
                "</color>"
    }
}