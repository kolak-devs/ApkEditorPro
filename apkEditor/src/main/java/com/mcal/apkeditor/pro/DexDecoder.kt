package com.mcal.apkeditor.pro

import org.jf.baksmali.Baksmali
import org.jf.baksmali.BaksmaliOptions
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import java.io.File

class DexDecoder(private val dexFilePath: String) {
    @JvmField
    var warning: String? = null

    @Throws(Exception::class)
    fun dex2smali(outputDirectory: String) {
        val dexFile = loadDexFile(dexFilePath)
        if (dexFile.supportsOptimizedOpcodes()) {
            warning = "You are disassembling an odex file without deodexing it. You won't be able to re-assemble the results unless you deodex it"
        }
        val outputDirectoryFile = File(outputDirectory)
        if (!outputDirectoryFile.exists()) {
            if (!outputDirectoryFile.mkdirs()) {
                throw Exception("Can't create the output directory $outputDirectory")
            }
        }
        val jobs = Runtime.getRuntime().availableProcessors()
        if (!Baksmali.disassembleDexFile(dexFile, outputDirectoryFile, jobs, options, null)) {
            throw Exception("Baksmali.disassembleDexFile failed.")
        }
    }

    @Throws(Exception::class)
    private fun loadDexFile(input: String): DexBackedDexFile {
        val file = File(input)
        if (!file.exists() || file.isDirectory) {
            throw Exception("Can't find file: $input")
        }
        return DexFileFactory.loadDexFile(file, Opcodes.forApi(15))
    }

    private val options: BaksmaliOptions
        get() {
            val options = BaksmaliOptions()
            options.parameterRegisters = true
            options.localsDirective = false
            options.sequentialLabels = false
            options.debugInfo = true
            options.codeOffsets = false
            options.accessorComments = true
            options.implicitReferences = false
            options.normalizeVirtualMethods = false
            options.registerInfo = 0
            return options
        }
}