package com.mcal.apkeditor.ui.fulleditor.utils

import brut.androlib.Androlib
import brut.androlib.ApkDecoder
import com.mcal.androlib.options.BuildOptions
import com.mcal.androlib.util.Logger
import com.mcal.apkeditor.ApkParseConsumer
import com.mcal.common.data.Preferences
import com.mcal.common.utils.ScopedStorage
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
            val options = BuildOptions()
            options.frameworkFolderLocation = binFolder
            val lib = Androlib(options, this)
            val decoder = ApkDecoder(apkPath, lib)
            decoder.setApkFile(apkPath)
            decoder.setBaksmaliDebugMode(false)
            decoder.setFrameworkDir(binFolder)
            if (Preferences.isNeedDecodeAssets()) {
                decoder.setDecodeAssets(ApkDecoder.DECODE_ASSETS_FULL)
            }
            decoder.setDecodeResources(if (Preferences.isNeedDecodeResources()) ApkDecoder.DECODE_RESOURCES_FULL else ApkDecoder.DECODE_RESOURCES_NONE)
            decoder.setDecodeSources(if (Preferences.isNeedDecodeClasses()) ApkDecoder.DECODE_SOURCES_SMALI else ApkDecoder.DECODE_SOURCES_NONE)
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