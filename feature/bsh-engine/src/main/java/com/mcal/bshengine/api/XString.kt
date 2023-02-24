package com.mcal.bshengine.api


class XString {
    /**
     * Конвертация UNICODE кодов в символы
     */
    fun unescapeUnicode(string: String): String = string.replace("\\\\u([\\dA-Fa-f]{4})".toRegex()) {
        String(Character.toChars(it.groupValues[1].toInt(radix = 16)))
    }
}