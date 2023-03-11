package com.mcal.common.utils

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import java.util.regex.Pattern
import kotlin.random.Random

/**
 * Если расширение файла есть в списке, то вернёт TRUE
 */
fun String.findExt(pattern: String): Boolean {
    return this.substring(this.lastIndexOf('.') + 1).matches(Regex(pattern))
}

private val letters = charArrayOf(
    'a', 'b', 'c', 'd', 'e',
    'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
    's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
)

fun getRandomString(bits: Int): String {
    val random = Random(System.currentTimeMillis())
    val sb = StringBuilder()
    for (i in 0 until bits) {
        val index = random.nextInt(letters.size)
        sb.append(letters[index])
    }
    return sb.toString()
}

fun join(join: String, strAry: Array<String>): String {
    val sb = java.lang.StringBuilder()
    for (i in strAry.indices) {
        if (i == strAry.size - 1) {
            sb.append(strAry[i])
        } else {
            sb.append(strAry[i]).append(join)
        }
    }
    return sb.toString()
}

fun setManifestHightLight(text: String): SpannableString {
    val syntaxNumAttribute = Color.parseColor("#1976d2")
    val syntaxString = Color.parseColor("#4caf50")
    val syntaxArtaNumAttribute = Color.parseColor("#693d94")
    val spanText = SpannableString(text)
    val length = text.length
    var start: Int
    var end: Int
    var matcher = Pattern.compile("\\s*</.*").matcher(text)
    // Красим "</application>"
    if (matcher.find()) {
        start = text.indexOf("</") + 2
        end = text.lastIndexOf(">")
        if (start >= 0 && end >= 0) {
            spanText.setSpan(
                ForegroundColorSpan(syntaxNumAttribute),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spanText.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    // Красим "<intent-filter>" и "</intent-filter>"
    matcher = Pattern.compile("\\s*</?\\w+-\\w+>").matcher(text)
    if (matcher.find()) {
        start = text.indexOf("</") + 2
        if (start >= 0) {
            end = text.indexOf(">")
            if (end >= 0) {
                spanText.setSpan(
                    ForegroundColorSpan(syntaxNumAttribute),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spanText.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } else {
            start = text.indexOf("<") + 1
            if (start >= 0) {
                end = text.indexOf(">") - 1
                if (end >= 0) {
                    spanText.setSpan(
                        ForegroundColorSpan(syntaxNumAttribute),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spanText.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    } else {
        // Красим "<application "
        matcher = Pattern.compile("\\s*<\\w+.*").matcher(text)
        if (matcher.find()) {
            start = text.indexOf("<") + 1
            if (start >= 0) {
                end = text.indexOf(" ")
                if (end >= 0) {
                    spanText.setSpan(
                        ForegroundColorSpan(syntaxNumAttribute),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spanText.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else {
                    spanText.setSpan(
                        ForegroundColorSpan(syntaxNumAttribute),
                        start,
                        length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spanText.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
    }
    if (text.contains('"')) {
        var count = 0
        do {
            start = text.indexOf('"', count)
            if (start >= 0) {
                end = text.indexOf('"', start + 1)
                if (end >= 0) {
                    count = end + 1
                    spanText.setSpan(
                        ForegroundColorSpan(syntaxString),
                        start,
                        end + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else {
                    break
                }
            } else {
                break
            }
        } while (true)
    }
    if (text.contains('\'')) {
        var count = 0
        do {
            start = text.indexOf('\'', count)
            if (start >= 0) {
                end = text.indexOf('\'', start + 1)
                if (end >= 0) {
                    count = end + 1
                    spanText.setSpan(
                        ForegroundColorSpan(syntaxString),
                        start,
                        end + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else {
                    break
                }
            } else {
                break
            }
        } while (true)
    }
    if (text.contains('=')) {
        var count = 0
        do {
            start = text.indexOf(':', count)
            if (start >= 0) {
                end = text.indexOf('=', start)
                if (end >= 0) {
                    count = end + 1
                    spanText.setSpan(
                        ForegroundColorSpan(syntaxArtaNumAttribute),
                        start + 1,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else {
                    break
                }
            } else {
                start = text.indexOf(' ', count)
                if (start >= 0) {
                    end = text.indexOf('=', start)
                    if (end >= 0) {
                        count = end + 1
                        spanText.setSpan(
                            ForegroundColorSpan(syntaxArtaNumAttribute),
                            start + 1,
                            end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    } else {
                        break
                    }
                } else {
                    if (text.indexOf(' ') < 0 && text.indexOf(':') < 0) {
                        end = text.indexOf('=')
                        if (end >= 0) {
                            spanText.setSpan(
                                ForegroundColorSpan(syntaxArtaNumAttribute),
                                0,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }
                    break
                }
            }
        } while (true)
    }
    return spanText
}