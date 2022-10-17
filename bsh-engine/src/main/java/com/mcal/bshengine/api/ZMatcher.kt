package com.mcal.bshengine.api

import android.content.Context
import java.util.regex.Pattern

class ZMatcher(context: Context) {
    /**
     * @param regex - регулярное выражение
     * @param source - текст в котором нужно найти группу и заменить
     * @param groupToReplace - в какой группе заменить текст
     * @param replacement - на что заменить
     */
    fun replaceGroup(regex: String, source: String, groupToReplace: Int, replacement: String): String {
        return replaceGroup(regex, source, groupToReplace, 1, replacement)
    }

    private fun replaceGroup(regex: String, source: String, groupToReplace: Int, @Suppress("SameParameterValue") groupOccurrence: Int, replacement: String): String {
        val m = Pattern.compile(regex).matcher(source)
        for (i in 0 until groupOccurrence) {
            if (!m.find()) {
                return source
            }
        }
        return StringBuilder(source).replace(m.start(groupToReplace), m.end(groupToReplace), replacement).toString()
    }
}