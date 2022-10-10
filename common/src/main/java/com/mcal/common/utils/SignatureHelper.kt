package com.mcal.common.utils

import android.sun.security.pkcs.PKCS7
import android.util.Base64
import java.io.*
import java.security.cert.CertificateEncodingException

object SignatureHelper {
    @JvmStatic
    fun getApkSignatureData(decodedPath: String): String {
        val path = File(decodedPath + File.separator + "original" + File.separator + "META-INF")
        if (!path.exists()) {
            return "META-INF not found!"
        }
        try {
            path.listFiles()?.forEach {
                val name = it.path
                if (name.endsWith(".RSA") || name.endsWith(".DSA")) {
                    val certificates = PKCS7(FileInputStream(name).readBytes()).certificates
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    val dataOutputStream = DataOutputStream(byteArrayOutputStream)
                    dataOutputStream.write(certificates.size)
                    for (i in certificates.indices) {
                        val encoded = certificates[i].encoded
                        dataOutputStream.writeInt(encoded.size)
                        dataOutputStream.write(encoded)
                    }
                    return Base64.encodeToString(byteArrayOutputStream.toByteArray(), 0).replace("\n", "\\n")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: CertificateEncodingException) {
            e.printStackTrace()
        }
        return "Get signature failed!"
    }
}