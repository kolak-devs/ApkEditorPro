package com.mcal.common.data

import kotlinx.coroutines.runBlocking


object Constants {
    const val LOG_ERROR = 1
    const val LOG_INFO = 0
    const val PACKAGE_NAME = "com.mcal.apkeditor.pro"
    const val EXTRACT_AUTORENAME = 0

    private const val DOMAIN_RU = "https://timscriptov.ru"
    private const val DOMAIN_COM = "https://timscriptov.com"

    const val MAXIMOFF = "https://raw.githubusercontent.com/Maximoff/binaries/main"
    val SDK_VERSIONS: IntRange = 22..36

    fun getDomain(): String {
        return DOMAIN_RU
    }

    fun getMaximoffBin(abi: String, name: String): String {
        return "$MAXIMOFF/bin/$abi/$name"
    }

    fun getMaximoffFramework(sdk: Int): String {
        return "$MAXIMOFF/bin/frameworks/sdk-$sdk.apk"
    }
}