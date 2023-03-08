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
     * Since: 2.4.7
     */
    fun smali2dex(smaliFilePathList: List<String>, outputDexPath: String): Boolean {
        return Smali.assemble(SmaliOptions().apply {
            outputDexFile = outputDexPath
        }, smaliFilePathList)
    }

    /**
     * Since: 2.4.7
     */
    fun smali2java(smaliFilePath: String, outputDir: File) {
        JadxDecompiler().use { decompiler ->
            decompiler.addCustomLoad(
                SmaliInputPlugin().loadFiles(
                    listOf<Path>(Paths.get(smaliFilePath))
                )
            )
            decompiler.load()
            decompiler.classes.forEach { javaClass ->
                writeText(File(outputDir, javaClass.name), javaClass.code)
            }
        }
    }

    /**
     * Since: 2.4.7
     */
    fun smali2java(smaliFilePath: List<String>, outputDir: File) {
        val smaliPathList = mutableListOf<Path>()
        smaliFilePath.forEach { smaliPath ->
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
                writeText(File(outputDir, javaClass.name), javaClass.code)
            }
        }
    }

    /**
     * @return result
     * Since: 2.4.7
     */
    fun dex2smali(dexInputStream: FileInputStream, outputDir: File): Boolean {
        return Baksmali.disassembleDexFile(
            DexBackedDexFile.fromInputStream(
                Opcodes.getDefault(),
                BufferedInputStream(dexInputStream)
            ),
            outputDir,
            Runtime.getRuntime().availableProcessors(),
            BaksmaliOptions()
        )
    }

    /**
     * Since: 2.4.7
     */
    fun dex2java(dexFile: File, outputJavaFile: File) {
        val args = JadxArgs()
        args.isSkipResources = true
        args.isShowInconsistentCode = true
        args.setInputFile(dexFile)
        args.outDirSrc = outputJavaFile
        val decompiler = JadxDecompiler(args)
        decompiler.load()
        decompiler.saveSources()
    }
}
