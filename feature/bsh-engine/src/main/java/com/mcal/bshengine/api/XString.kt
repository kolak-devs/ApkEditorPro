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

    /**
     * Since: 2.4.6
     */
    fun isNotEmpty(string: String): Boolean {
        return string.isNotEmpty()
    }

    /**
     * Since: 2.4.6
     */
    fun isEmpty(string: String): Boolean {
        return string.isEmpty()
    }

    /**
     * Since: 2.4.6
     */
    fun isNullOrEmpty(string: String?): Boolean {
        return string.isNullOrEmpty()
    }

    /**
     * Since: 2.4.6
     */
    fun isNotBlank(string: String): Boolean {
        return string.isNotBlank()
    }

    /**
     * Since: 2.4.6
     */
    fun isBlank(string: String): Boolean {
        return string.isBlank()
    }

    /**
     * Since: 2.4.6
     */
    fun isNullOrBlank(string: String?): Boolean {
        return string.isNullOrBlank()
    }
}