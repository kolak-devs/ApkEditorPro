package com.mcal.common.utils

import android.os.Build
import android.util.Log

object CPUArch {
    private const val ARCH_ARM_32 = 0x1
    private const val ARCH_ARM_64 = 0x2
    private const val ARCH_X86_32 = 0x3
    private const val ARCH_X86_64 = 0x4
    private const val ARCH_OTHERS = 0x0
    private const val ARCH_NAME_ARM_32 = "armeabi-v7a"
    private const val ARCH_NAME_ARM_64 = "arm64-v8a"
    private const val ARCH_NAME_X86_32 = "x86"
    private const val ARCH_NAME_X86_64 = "x86_64"
    private const val ARCH_NAME_OTHERS = "others"

    fun getBitForABI(str: String): Int {
        return when (str) {
            "armeabi" -> 1 shl 0
            "armeabi-v7a" -> 1 shl 1
            "arm64-v8a" -> 1 shl 2
            "x86" -> 1 shl 3
            "x86_64" -> 1 shl 4
            "mips" -> 1 shl 5
            "mips64" -> 1 shl 6
            else -> {
                Log.w(this.javaClass.name, "Unknown ABI: $str")
                0
            }
        }
    }

    fun getArchName(archId: Int): String {
        when (archId) {
            ARCH_ARM_32 -> return ARCH_NAME_ARM_32
            ARCH_ARM_64 -> return ARCH_NAME_ARM_64
            ARCH_X86_32 -> return ARCH_NAME_X86_32
            ARCH_X86_64 -> return ARCH_NAME_X86_64
        }
        return ARCH_NAME_OTHERS
    }

    val systemSupportedAbi: Array<String>
        get() = Build.SUPPORTED_ABIS

    fun is64BitArch(abiName: String): Boolean {
        return abiName == ARCH_NAME_ARM_64 || abiName == ARCH_NAME_X86_64
    }
}