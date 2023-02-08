package com.mcal.apkeditor.ui.fulleditor.utils

import brut.androlib.Androlib
import brut.androlib.ApkDecoder
import com.mcal.androlib.options.BuildOptions
import com.mcal.androlib.utils.Logger
import com.mcal.apkeditor.ApkParseConsumer
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.utils.ScopedStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.logging.Level

class TaskDecoder : Logger {
    val fileEntry2ZipEntry: Map<String, String> = HashMap()

    fun decode(consumer: ApkParseConsumer?, apkFile: File, decodePath: File) {
        decode(apkFile, decodePath)
        consumer?.resourceDecoded(fileEntry2ZipEntry)
        consumer?.resTableDecoded(true)
    }

    private fun decode(apkPath: File, decodeRootPath: File) {
        val binFolder = ScopedStorage.getBinDir().path
        try {
            val lib = Androlib(BuildOptions().apply {
                frameworkFolderLocation = binFolder
                aaptPath = binFolder + File.separator + "aapt"
                aapt2Path = binFolder + File.separator + "aapt2"
                isAaptRules = ReactivePreferences.isAaptRules()
                isJsonConfig = ReactivePreferences.isJsonConfig()
            }, this)
            val decoder = ApkDecoder(apkPath, lib)
            decoder.setApkFile(apkPath)
            decoder.setBaksmaliDebugMode(false)
            decoder.setFrameworkDir(binFolder)
            CoroutineScope(Dispatchers.Main).launch {
                if (ReactivePreferences.isNeedDecodeAssets()) {
                    decoder.setDecodeAssets(ApkDecoder.DECODE_ASSETS_FULL)
                }
                decoder.setDecodeResources(if (ReactivePreferences.isNeedDecodeResources()) ApkDecoder.DECODE_RESOURCES_FULL else ApkDecoder.DECODE_RESOURCES_NONE)
                decoder.setDecodeSources(if (ReactivePreferences.isNeedDecodeClasses()) ApkDecoder.DECODE_SOURCES_SMALI else ApkDecoder.DECODE_SOURCES_NONE)
            }
            decoder.setOutDir(decodeRootPath)
            decoder.setApiLevel(14)
            decoder.setForceDelete(true)
            decoder.decode()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun error(log: String) = Unit
    override fun log(warring: Level, format: String, ex: Throwable) = Unit
    override fun fine(log: String) = Unit
    override fun warning(log: String) = Unit
    override fun info(log: String) = Unit
}