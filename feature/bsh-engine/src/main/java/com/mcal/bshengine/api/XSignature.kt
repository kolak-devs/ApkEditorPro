package com.mcal.bshengine.api

import com.mcal.common.utils.SignatureHelper.getApkSignatureData
import java.io.File

class XSignature(private var decodedDir: String) {
    /**
     * Since: 2.4.5
     */
    fun getSignature(): String {
        return getApkSignatureData(File(decodedDir).path)
    }
}