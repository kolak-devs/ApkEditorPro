package com.mcal.common.data

import com.mcal.common.data.Preferences.isDomainCom

object Constants {
    const val PACKAGE_NAME = "com.mcal.apkeditor.pro"
    const val EXTRACT_AUTORENAME = 0

    private const val DOMAIN_RU = "https://timscriptov.ru"
    private const val DOMAIN_COM = "https://timscriptov.com"

    fun getDomain(): String {
        return if (isDomainCom()) DOMAIN_COM else DOMAIN_RU
    }
}