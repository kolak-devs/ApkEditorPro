package com.mcal.bshengine.api

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets

class ZCipher(context: Context) {
    fun decodeBase64(text: String): String {
        return String(Base64.decode(text, Base64.DEFAULT), StandardCharsets.UTF_8)
    }

    fun encodeBase64(text: String): String {
        return Base64.encodeToString(text.toByteArray(StandardCharsets.UTF_8), Base64.DEFAULT)
    }
}