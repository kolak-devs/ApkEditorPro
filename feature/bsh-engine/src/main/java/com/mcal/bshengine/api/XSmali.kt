package com.mcal.bshengine.api

import com.mcal.editor.utils.FileUtils.writeText
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.plugins.input.smali.SmaliInputPlugin
import org.jf.baksmali.Baksmali
import org.jf.baksmali.BaksmaliOptions
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.smali.Smali
import org.jf.smali.SmaliOptions
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path
import java.nio.file.Paths

class XSmali {
    /**
     * @return result
     * Since: 2.4.7
     */
    fun smaliDir2dex(smaliDir: File, outputDexFile: File): Boolean {
        val smaliFilePathList = arrayListOf<String>()
        smaliDir.walk().filter { it.isFile }.forEach { smaliFile ->
            val path = smaliFile.path
            if (path.endsWith(".smali")) {
                smaliFilePathList.add(path)
            }
        }
        return smali2dex(smaliFilePathList, outputDexFile)
    }

    /**
     * @return result
     * Since: 2.4.7
     */
    fun smali2dex(smaliFile: File, outputDexFile: File): Boolean {
        return smali2dex(arrayListOf<String>(smaliFile.path), outputDexFile)
    }

    /**
     * @return result
     * Since: 2.4.7
     */
    fun smali2dex(smaliFilePathList: List<String>, outputDexFile: File): Boolean {
        return Smali.assemble(SmaliOptions().apply {
            this.outputDexFile = outputDexFile.path
        }, smaliFilePathList)
    }

    /**
     * Since: 2.4.7
     */
    fun smaliDir2java(smaliDir: File, outputDir: File) {
        val smaliFilePathList = arrayListOf<String>()
        smaliDir.walk().filter { it.isFile }.forEach { smaliFile ->
            val path = smaliFile.path
            if (path.endsWith(".smali")) {
                smaliFilePathList.add(path)
            }
        }
        smali2java(smaliFilePathList, outputDir)
    }

    /**
     * Since: 2.4.7
     */
    fun smali2java(smaliFile: File, outputJavaDir: File) {
        smali2java(arrayListOf(smaliFile.path), outputJavaDir)
    }

    /**
     * Since: 2.4.7
     */
    fun smali2java(smaliFilePathList: List<String>, outputJavaDir: File) {
        val smaliPathList = mutableListOf<Path>()
        smaliFilePathList.forEach { smaliPath ->
            smaliPathList.add(Paths.get(smaliPath))
        }
        JadxDecompiler().use { decompiler ->
            decompiler.addCustomLoad(
                SmaliInputPlugin().loadFiles(
                    smaliPathList
                )
            )
            decompiler.load()
            decompiler.classes.forEach { javaClass ->
                writeText(File(File(outputJavaDir, javaClass.getPackage().replace(".", "/")), javaClass.name), javaClass.code)
            }
        }
    }

    /**
     * @return result
     * Since: 2.4.7
     */
    fun dex2smali(dexFile: File, outputSmaliDir: File): Boolean {
        return Baksmali.disassembleDexFile(
            DexBackedDexFile.fromInputStream(
                Opcodes.getDefault(),
                BufferedInputStream(FileInputStream(dexFile))
            ),
            outputSmaliDir,
            Runtime.getRuntime().availableProcessors(),
            BaksmaliOptions()
        )
    }

    /**
     * Since: 2.4.7
     */
    fun dex2java(dexFile: File, outputJavaDir: File) {
        val args = JadxArgs()
        args.isSkipResources = true
        args.isShowInconsistentCode = true
        args.setInputFile(dexFile)
        args.outDirSrc = outputJavaDir
        val decompiler = JadxDecompiler(args)
        decompiler.load()
        decompiler.saveSources()
    }
}
