package com.mcal.bshengine.api


object XString {
    /**
     * Конвертация UNICODE кодов в символы
     */
    @JvmStatic
    fun unescapeUnicode(string: String) = string.replace("\\\\u([\\dA-Fa-f]{4})".toRegex()) {
        String(Character.toChars(it.groupValues[1].toInt(radix = 16)))
    }
}