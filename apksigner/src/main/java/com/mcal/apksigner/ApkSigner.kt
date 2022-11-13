package com.mcal.apksigner

import com.android.apksig.ApkSigner
import com.android.apksigner.ApkSignerTool
import com.mcal.apksigner.utils.JksKeyStore
import com.mcal.apksigner.utils.LoadKeystoreException
import com.mcal.common.data.LegacyPreferences
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.ScopedStorage.filesDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate

class ApkSigner {
    suspend fun signApk(inputPath: String, outputPath: String): Boolean = withContext(Dispatchers.IO) {
        val args = mutableListOf(
            "sign",
            "--in",
            inputPath,
            "--out",
            outputPath,
            "--key",
            filesDir.toString() + File.separator + "bin/testkey.pk8",
            "--cert",
            filesDir.toString() + File.separator + "bin/testkey.x509.pem"
        )
        try {
            ApkSignerTool.main(args.toTypedArray())
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun signApkCustom(inputPath: String, outputPath: String) : Boolean {
        return sign(File(inputPath), File(outputPath))
    }

    private suspend fun sign(input: File, out: File): Boolean = withContext(Dispatchers.IO) {
        try {
            ScopedStorage.getKey()?.takeIf { it.exists() }?.let {
                val keystore = loadKeyStore(it.path, ReactivePreferences.getSigningPassword().toCharArray())
                val certAlias = ReactivePreferences.getKeyAlias()
                val signerConfig = ApkSigner.SignerConfig.Builder(
                    "CERT",
                    keystore.getKey(certAlias, ReactivePreferences.getKeyPassword().toCharArray()) as PrivateKey,
                    listOf(keystore.getCertificate(certAlias) as X509Certificate)
                ).build()
                ApkSigner.Builder(listOf(signerConfig)).apply {
                    setInputApk(input)
                    setOutputApk(out)
                    when (ReactivePreferences.getSigningVersion()){
                        1 -> setV1SigningEnabled(true)
                        2 -> {
                            setV1SigningEnabled(true)
                            setV2SigningEnabled(true)
                        }
                        3 -> {
                            setV1SigningEnabled(true)
                            setV2SigningEnabled(true)
                            setV3SigningEnabled(true)
                        }
                        4 -> {
                            setV1SigningEnabled(true)
                            setV2SigningEnabled(true)
                            setV3SigningEnabled(true)
                            setV4SigningEnabled(true)
                        }
                    }
                }.build().sign()
                return@withContext true
            } ?: run {
                throw FileNotFoundException("KeyStore file not found.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    @Throws(Exception::class)
    private fun loadKeyStore(keystorePath: String, password: CharArray): KeyStore {
        val provider = BouncyCastleProvider()
        Security.addProvider(provider)
        var ks: KeyStore
        return try {
            ks = JksKeyStore(provider)
            val fis = FileInputStream(keystorePath)
            ks.load(fis, password)
            fis.close()
            ks
        } catch (e: LoadKeystoreException) {
            throw e
        } catch (e: Exception) {
            try {
                ks = KeyStore.getInstance("bks", provider)
                val fis = FileInputStream(keystorePath)
                ks.load(fis, password)
                fis.close()
                ks
            } catch (e: Exception) {
                throw RuntimeException("Failed to load keystore: " + e.message, e)
            }
        }
    }
}