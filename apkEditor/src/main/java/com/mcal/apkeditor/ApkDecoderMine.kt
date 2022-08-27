package com.mcal.apkeditor

import android.app.Activity
import brut.androlib.Androlib
import brut.androlib.AndrolibException
import brut.androlib.ApkDecoder
import brut.androlib.options.BuildOptions
import brut.androlib.res.data.ResPackage
import brut.androlib.res.data.ResTable
import brut.androlib.res.decoder.Res9patchStreamDecoder
import brut.androlib.res.decoder.ResRawStreamDecoder
import brut.androlib.res.decoder.ResStreamDecoderContainer
import com.mcal.androlib.util.Logger
import com.mcal.apkeditor.utils.AssetsInstaller
import com.mcal.apklib.AXMLParser.IReferenceDecode
import org.jetbrains.annotations.Contract
import java.io.File
import java.util.logging.Level

class ApkDecoderMine(val resTable: ResTable) : IReferenceDecode, Logger {

    // Record all the file entry to zip entry
    // like res/drawable-hdpi-v4/a.png -> res/drawable-hdpi/a.png
    val fileEntry2ZipEntry: Map<String, String> = HashMap()
    fun decode(activity: Activity, apkPath: File, decodeRootPath: File) {
        val binFolder = File(activity.filesDir.toString() + "/bin")

        // Preparing
        try {
            AssetsInstaller(activity).install()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val options = BuildOptions()
            options.frameworkFolderLocation = binFolder.path
            val lib = Androlib(options, this)
            val decoder = ApkDecoder(apkPath, lib)
            decoder.setApkFile(apkPath)
            decoder.setBaksmaliDebugMode(false)
            decoder.setFrameworkDir(binFolder.path) //android-framework.jar
            //decoder.setDecodeAssets(ApkDecoder.DECODE_ASSETS_FULL);
            decoder.setDecodeResources(ApkDecoder.DECODE_RESOURCES_FULL)
            //decoder.setDecodeResources(ApkDecoder.DECODE_RESOURCES_NONE);
            //decoder.setDecodeSources(ApkDecoder.DECODE_SOURCES_SMALI);
            decoder.setDecodeSources(ApkDecoder.DECODE_SOURCES_NONE)
            decoder.setOutDir(decodeRootPath)
            decoder.setApiLevel(14)
            decoder.setForceDelete(true)
            decoder.decode()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val resPackage: ResPackage?
        get() {
            val packages = resTable.listMainPackages()
            return if (packages != null && packages.isNotEmpty()) {
                packages.iterator().next()
            } else null
        }

    override fun getResReference(id: Int): String {
        try {
            val spec = resTable.getResSpec(id)
            return String.format(
                "@%s%s/%s", getPackageName(id), spec.type,
                spec.name
            )
        } catch (e: AndrolibException) {
            e.printStackTrace()
        }
        return String.format("@%s%08X", getPackageName(id), id)
    }

    fun stopDecode() {
        // TODO IMPLEMENT ME
    }

    override fun error(log: String) = Unit
    override fun log(warring: Level, format: String, ex: Throwable) = Unit
    override fun fine(log: String) = Unit
    override fun warning(log: String) = Unit
    override fun info(log: String) = Unit

    companion object {
        @Contract(pure = true)
        private fun getPackageName(id: Int): String {
            return if (id ushr 24 == 1) {
                "android:"
            } else ""
        }
    }

    init {
        val xmlDecoder = XmlDecoder(resPackage)
        val mDecoders = ResStreamDecoderContainer()
        mDecoders.setDecoder("xml", xmlDecoder)
        val res9pathDecoder = Res9patchStreamDecoder()
        mDecoders.setDecoder("9path", res9pathDecoder)
        val rawDecoder = ResRawStreamDecoder()
        mDecoders.setDecoder("raw", rawDecoder)
    }
}