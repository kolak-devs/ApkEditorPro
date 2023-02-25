package com.mcal.bshengine.api


class XString {
    /**
     * Конвертация UNICODE кодов в символы
     *
     * Since: 2.4.5
     */
    fun unescapeUnicode(string: String): String = string.replace("\\\\u([\\dA-Fa-f]{4})".toRegex()) {
        String(Character.toChars(it.groupValues[1].toInt(radix = 16)))
    }
}