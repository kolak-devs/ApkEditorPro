package com.mcal.apksigner

import com.android.apksig.ApkSigner
import com.android.apksigner.ApkSignerTool
import com.mcal.apksigner.utils.JksKeyStore
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.ScopedStorage.filesDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.withContext
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.util.concurrent.CompletableFuture

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

    suspend fun signApkCustom(inputPath: String, outputPath: String): Boolean {
        return sign(File(inputPath), File(outputPath))
    }

    private suspend fun sign(input: File, out: File): Boolean = withContext(Dispatchers.IO) {
        try {
            ScopedStorage.getKey()?.takeIf { it.exists() }?.let { keyFile ->
                val keystore = loadKeyStore(FileInputStream(keyFile), ReactivePreferences.getSigningPassword().toCharArray())
                val certAlias = ReactivePreferences.getKeyAlias()
                val signerConfig = ApkSigner.SignerConfig.Builder(
                    "CERT",
                    keystore.getKey(certAlias, ReactivePreferences.getKeyPassword().toCharArray()) as PrivateKey,
                    listOf(keystore.getCertificate(certAlias) as X509Certificate)
                ).build()
                ApkSigner.Builder(listOf(signerConfig)).apply {
                    setInputApk(input)
                    setOutputApk(out)
                    when (ReactivePreferences.getSigningVersion()) {
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
    private fun loadKeyStore(keystorePath: FileInputStream, password: CharArray): KeyStore {
        var keyStore: KeyStore
        try {
            keyStore = KeyStore.getInstance("jks")
            keyStore.load(keystorePath, password)
        } catch (e: Exception) {
            val provider = BouncyCastleProvider()
            Security.addProvider(provider)
            try {
                keyStore = JksKeyStore(provider)
                keyStore.load(keystorePath, password)
            } catch (e: Exception) {
                try {
                    keyStore = KeyStore.getInstance("bks", provider)
                    keyStore.load(keystorePath, password)
                } catch (e: Exception) {
                    throw RuntimeException("Failed to load keystore: " + e.message)
                }
            }
        } finally {
            keystorePath.close()
        }
        return keyStore
    }

    // Helper to call coroutines from java
    fun signAsync(inputPath: String, outputPath: String): CompletableFuture<Boolean> =
        GlobalScope.future { signApk(inputPath, outputPath) }
}