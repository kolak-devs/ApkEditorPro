package com.mcal.apkeditor.ui.fulleditor.utils

import brut.androlib.Androlib
import brut.androlib.ApkDecoder
import com.mcal.androlib.options.BuildOptions
import com.mcal.androlib.utils.Logger
import com.mcal.apkeditor.ApkParseConsumer
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.utils.ScopedStorage
import com.android.tools.smali.baksmali.Baksmali
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.util.logging.Level
import java.util.zip.ZipFile

class TaskDecoder : Logger {
    val fileEntry2ZipEntry: Map<String, String> = HashMap()

    fun decode(consumer: ApkParseConsumer?, apkFile: File, decodePath: File) {
        decode(apkFile, decodePath)
        consumer?.resourceDecoded(fileEntry2ZipEntry)
        consumer?.resTableDecoded(true)
    }

    private fun ensureFramework() {
        val frameworkFile = File(ScopedStorage.getBinDir(), "1.apk")
        if (frameworkFile.isFile && frameworkFile.length() > 0) {
            return
        }
        val androidJar = ScopedStorage.getFramework()
        if (!androidJar.isFile || androidJar.length() <= 0) {
            Log.e("TaskDecoder", "Android framework not found in bin dir. Download it via Tools Manager first.")
            return
        }
        try {
            ZipFile(androidJar).use { }
            androidJar.copyTo(frameworkFile, overwrite = true)
            Log.i("TaskDecoder", "Installed framework ${androidJar.name} -> ${frameworkFile.name}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun decode(apkPath: File, decodeRootPath: File) {
        val binFolder = ScopedStorage.getBinDir().path
        try {
            if (runBlocking { ReactivePreferences.isNeedDecodeResources() }) {
                ensureFramework()
            }
            val lib = Androlib(BuildOptions().apply {
                frameworkFolderLocation = binFolder
                aaptPath = ScopedStorage.getAapt().path
                aapt2Path = ScopedStorage.getAapt2().path
                useNewBuildRules = ReactivePreferences.isAaptRulesAsync()
                useJsonConfig = ReactivePreferences.isJsonConfig()
                checkExistsFiles = ReactivePreferences.isCheckExistsFilesEnabledAsync()
                ignoreMultiRes = ReactivePreferences.ignoreMultiResAsync()
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
            decoder.setForceDelete(true)
            decoder.decode()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (runBlocking { ReactivePreferences.isNeedDecodeClasses() }) {
                try {
                    ZipFile(apkPath).use { zip ->
                        zip.entries().asSequence().filter { entry ->
                            !entry.isDirectory && entry.name.endsWith(".dex") && !entry.name.contains("/")
                        }.forEach { entry ->
                            val name = entry.name
                            val smaliDir = File(
                                decodeRootPath,
                                if (name == "classes.dex") "smali" else "smali_" + name.removeSuffix(".dex")
                            )
                            if (smaliDir.isDirectory && smaliDir.walkTopDown().any { it.extension == "smali" }) {
                                return@forEach
                            }
                            info("Decompiling $name ...")
                            zip.getInputStream(entry).use { input ->
                                if (Baksmali.disassembleDexFile(
                                        DexBackedDexFile.fromInputStream(
                                            Opcodes.getDefault(),
                                            BufferedInputStream(input)
                                        ),
                                        smaliDir,
                                        Runtime.getRuntime().availableProcessors(),
                                        BaksmaliOptions()
                                    )
                                ) {
                                    File(decodeRootPath, name).delete()
                                }
                            }
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    override fun error(log: String) = Unit
    override fun log(warring: Level, format: String, ex: Throwable) = Unit
    override fun fine(log: String) = Unit
    override fun warning(log: String) = Unit
    override fun info(log: String) = Unit
}