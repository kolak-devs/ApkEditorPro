package com.mcal.apkeditor.pro

import com.android.tools.smali.smali.Smali
import com.android.tools.smali.smali.SmaliOptions
import java.io.File

object DexEncoder {
    @Throws(Exception::class)
    fun smali2Dex(srcDirectory: String, outputDexFile: String) {
        val smaliFileList = arrayListOf<String>()
        getSmaliFilesInDir(File(srcDirectory), smaliFileList)
        val options = SmaliOptions().apply {
            jobs = Runtime.getRuntime().availableProcessors()
            apiLevel = 15
            this.outputDexFile = outputDexFile
            allowOdexOpcodes = false
            verboseErrors = false
        }
        if (!Smali.assemble(options, smaliFileList)) {
            throw Exception("Encountered errors while compiling smali files.")
        }
    }

    private fun getSmaliFilesInDir(dir: File, smaliFiles: MutableList<String>) {
        val files = dir.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    getSmaliFilesInDir(file, smaliFiles)
                } else if (file.name.endsWith(".smali")) {
                    smaliFiles.add(file.path)
                }
            }
        }
    }
}