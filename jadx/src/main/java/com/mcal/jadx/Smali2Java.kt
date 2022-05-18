package com.mcal.jadx

import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import java.io.File
import java.lang.Exception

object Smali2Java {
    @JvmStatic
    fun decompile(errorMessage: String?, dexPath: String?, outputDir: String?): Boolean {
        var errorMessage = errorMessage
        try {
            val dexInputFile = File(dexPath)
            val javaOutputDir = File(outputDir)
            val args = JadxArgs()
            args.outDirSrc = javaOutputDir
            args.isSkipResources = true
            args.isShowInconsistentCode = true
            args.setInputFile(dexInputFile)
            val decompiler = JadxDecompiler(args)
            decompiler.load()
            decompiler.saveSources()
            val javaClass = decompiler.classes.iterator().next()
            javaClass.decompile()
            //javaClass.getCode();
            return true
        } catch (e: Exception) {
            errorMessage = "Cannot decompile java code: " + e.message
            e.printStackTrace()
        } catch (e: StackOverflowError) {
            errorMessage = "Cannot decompile java code: " + e.message
            e.printStackTrace()
        }
        return false
    }
}