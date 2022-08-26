package com.mcal.common.utils

import kotlin.random.Random


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