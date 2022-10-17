package com.mcal.bshengine.api

import android.content.Context
import com.mcal.common.utils.SignatureHelper.getApkSignatureData
import java.io.File

class ZSignature(context: Context, private var decodedDir: String) {
    fun getSignature(): String {
        return getApkSignatureData(File(decodedDir).path)
    }
}