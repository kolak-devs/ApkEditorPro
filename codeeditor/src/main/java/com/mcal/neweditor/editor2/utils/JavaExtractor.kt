package com.mcal.neweditor.editor2.utils

import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.plugins.input.dex.DexInputPlugin
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.DexFile
import org.jf.dexlib2.immutable.ImmutableDexFile
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class JavaExtractor(
    private val mApkPath: String,
    private val mDexName: String,
    private val mClassName: String,
    workingDirectory: String
) : IJavaExtractor {

    private val mWorkingDirectory: File
    private var mInterestedName: String
    private var errorMessage: String? = null

    private fun decompile(code: File?, targetFilePath: File): Boolean {
        return try {
            if (code != null) {
                val args = JadxArgs()
                args.isSkipResources = true
                args.isShowInconsistentCode = true
                args.setInputFile(code)
                args.outDirSrc = targetFilePath
                val decompiler = JadxDecompiler(args)
                decompiler.load()
                decompiler.saveSources()
                writeDexFile(code, targetFilePath)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Cannot decompile java code: " + e.message
            false
        } catch (e: StackOverflowError) {
            e.printStackTrace()
            errorMessage = "Cannot decompile java code: " + e.message
            false
        }
    }

    @Throws(IOException::class)
    fun writeDexFile(dex: File?, targetFilePath: File) {
        dex?.let {
            JadxDecompiler().use { decompiler ->
                FileInputStream(dex).use { input ->
                    decompiler.addCustomLoad(DexInputPlugin().loadDexFromInputStream(input, "$mWorkingDirectory/extracted.dex"))
                    decompiler.load()
                    for (cls in decompiler.classes) {
                        val path = File(targetFilePath.toString() + File.separator + cls.getPackage().replace(".", "/"))
                        if (!path.exists()) {
                            path.mkdirs()
                        }
                        FileUtils.writeText(path.toString() + File.separator + cls.name + ".java", cls.code)
                    }
                }
            }
        }
    }

    override fun extract(): Boolean {
        if (extractDex()) {
            val dexPath = File("$mWorkingDirectory/extracted.dex")
            return decompile(dexPath, mWorkingDirectory)
        }
        return false
    }

    override fun getErrorMessage(): String? {
        return errorMessage
    }

    private fun extractDex(): Boolean {
        // Load dex file
        var dexFile: DexFile
        try {
            dexFile = DexFileFactory.loadDexEntry(
                File(mApkPath),
                mDexName,
                true,
                Opcodes.forApi(15)
            ).dexFile
        } catch (e: Exception) {
            errorMessage = "The dex file cannot be decompiled."
            return false
        }

        // Filter classes
        val classes: MutableList<ClassDef> = ArrayList()
        for (classDef in dexFile.classes) {
            val currentClass = classDef.type
            if (currentClass.startsWith(mInterestedName)) {
                classes.add(classDef)
            }
        }

        // Check directory
        val dir = mWorkingDirectory
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dexFile = ImmutableDexFile(Opcodes.forApi(15), classes)
        try {
            DexFileFactory.writeDexFile("$mWorkingDirectory/extracted.dex", dexFile)
        } catch (e: Exception) {
            errorMessage = "Cannot extract " + mClassName +
                    " as dex extract failed: " + e.message
            return false
        }
        return true
    }

    init {
        mWorkingDirectory = File(workingDirectory)
        mInterestedName = mClassName
        val position = mClassName.lastIndexOf('$')
        if (position != -1) {
            mInterestedName = mClassName.substring(0, position)
        }
    }
}