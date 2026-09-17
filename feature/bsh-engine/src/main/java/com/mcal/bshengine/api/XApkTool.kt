package com.mcal.bshengine.api

import brut.androlib.Androlib
import brut.androlib.ApkDecoder
import com.mcal.androlib.options.BuildOptions
import com.mcal.androlib.utils.Logger
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.utils.ScopedStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.logging.Level

class XApkTool(private val xLog: XLog) {
    private var mUseNewAaptRules = true
    private var mUseJsonConfig = true
    private var mUseAapt2 = true
    private lateinit var mAaptPath: String
    private lateinit var mAapt2Path: String
    private lateinit var mFrameworkFolderLocation: String
    private var mIgnoreMultiRes = true
    private fun logger(): Logger {
        return object : Logger {
            override fun error(args: String?) {
                xLog.error(String.format("E: %s", args))
            }

            override fun log(level: Level, format: String?, ex: Throwable?) {
                val ch = level.name[0]
                val fmt = "%c: %s"
                format?.let {
                    xLog.info(String.format(fmt, ch, format))
                }
                log(fmt, ch, ex)
            }

            private fun log(fmt: String, ch: Char, ex: Throwable?) {
                if (ex == null) {
                    return
                }
                xLog.info(String.format(fmt, ch, ex.message))
                for (ste in ex.stackTrace) {
                    xLog.info(String.format(fmt, ch, ste))
                }
                log(fmt, ch, ex.cause)
            }

            override fun fine(args: String?) {
                xLog.info(String.format("F: %s", args))
            }

            override fun warning(args: String?) {
                xLog.info(String.format("W: %s", args))
            }

            override fun info(args: String?) {
                xLog.info(String.format("I: %s", args))
            }
        }
    }

    private fun options(): BuildOptions {
        return BuildOptions().apply {
            this.useNewBuildRules = mUseNewAaptRules
            this.useJsonConfig = mUseJsonConfig
            this.useAapt2 = mUseAapt2
            this.aaptPath = mAaptPath
            this.aapt2Path = mAapt2Path
            this.frameworkFolderLocation = mFrameworkFolderLocation
            this.ignoreMultiRes = mIgnoreMultiRes
        }
    }

    /**
     * Since: 2.4.7
     */
    fun decode(apkPath: File, decodeRootPath: File) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                ApkDecoder(apkPath, Androlib(options(), logger())).apply {
                    setApkFile(apkPath)
                    setBaksmaliDebugMode(false)
                    setFrameworkDir(ScopedStorage.getBinDir().path)
                    setDecodeResources(ApkDecoder.DECODE_RESOURCES_FULL)
                    setDecodeSources(ApkDecoder.DECODE_SOURCES_SMALI)
                    setOutDir(decodeRootPath)
                    setForceDelete(true)
                }.decode()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Since: 2.4.7
     */
    fun build(decodeDir: File, outputApkFile: File) {
        CoroutineScope(Dispatchers.IO).launch {
            Androlib(options(), logger()).build(decodeDir, outputApkFile)
        }
    }

    /**
     * Since: 2.4.7
     */
    fun setUseNewAaptRules(mode: Boolean): XApkTool {
        mUseNewAaptRules = mode
        return this
    }

    /**
     * Since: 2.4.7
     */
    fun setUseJsonConfig(mode: Boolean): XApkTool {
        mUseJsonConfig = mode
        return this
    }

    /**
     * Since: 2.4.7
     */
    fun setIgnoreMultiRes(mode: Boolean): XApkTool {
        mIgnoreMultiRes = mode
        return this
    }

    /**
     * Since: 2.4.7
     */
    fun setAaptPath(path: String): XApkTool {
        mAaptPath = path
        return this
    }

    /**
     * Since: 2.4.7
     */
    fun setAapt2Path(path: String): XApkTool {
        mAapt2Path = path
        return this
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            mUseNewAaptRules = ReactivePreferences.isAaptRules()
            mUseJsonConfig = ReactivePreferences.isJsonConfig()
            mUseAapt2 = ReactivePreferences.isAapt2()
            mAaptPath = ScopedStorage.getAapt().path
            mAapt2Path = ScopedStorage.getAapt2().path
            mFrameworkFolderLocation = ScopedStorage.getBinDir().path
        }
    }
}