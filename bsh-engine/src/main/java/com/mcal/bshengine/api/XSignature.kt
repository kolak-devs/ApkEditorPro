package com.mcal.bshengine.api

import com.mcal.common.utils.SignatureHelper.getApkSignatureData
import java.io.File

class XSignature(private var decodedDir: String) {
    fun getSignature(): String {
        return getApkSignatureData(File(decodedDir).path)
    }
}