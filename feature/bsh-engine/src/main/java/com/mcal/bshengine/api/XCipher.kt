package com.mcal.bshengine.api

import android.util.Base64
import java.nio.charset.StandardCharsets

class XCipher {
    fun decodeBase64(text: String): String {
        return String(Base64.decode(text, Base64.NO_WRAP), StandardCharsets.UTF_8)
    }

    fun encodeBase64(text: String): String {
        return Base64.encodeToString(text.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
    }
}