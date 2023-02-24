package com.mcal.bshengine.api

import android.util.Base64
import java.nio.charset.StandardCharsets

object XCipher {
    @JvmStatic
    fun decodeBase64(text: String): String {
        return String(Base64.decode(text, Base64.DEFAULT), StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun encodeBase64(text: String): String {
        return Base64.encodeToString(text.toByteArray(StandardCharsets.UTF_8), Base64.DEFAULT)
    }
}