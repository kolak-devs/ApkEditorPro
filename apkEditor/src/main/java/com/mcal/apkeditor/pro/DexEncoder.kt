package com.mcal.apkeditor.pro

import com.google.common.collect.Lists
import org.jf.smali.SmaliOptions
import com.mcal.apkeditor.pro.DexEncoder
import org.jf.dexlib2.writer.builder.DexBuilder
import org.jf.dexlib2.writer.io.FileDataStore
import org.jf.smali.smaliFlexLexer
import org.antlr.runtime.CommonTokenStream
import org.jf.smali.smaliParser
import org.jf.smali.smaliParser.smali_file_return
import org.antlr.runtime.tree.CommonTree
import org.antlr.runtime.tree.CommonTreeNodeStream
import org.jf.dexlib2.Opcodes
import org.jf.smali.smaliTreeWalker
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.RuntimeException
import java.nio.charset.StandardCharsets
import java.util.LinkedHashSet
import java.util.concurrent.Executors
import java.util.concurrent.Future

object DexEncoder {
    @Throws(Exception::class)
    fun smali2Dex(srcDirectory: String, outputDexFile: String) {
        val options = SmaliOptions()
        options.jobs = Runtime.getRuntime().availableProcessors()
        options.apiLevel = 15
        options.outputDexFile = outputDexFile
        options.allowOdexOpcodes = false
        options.verboseErrors = false
        try {
            val filesToProcessSet = LinkedHashSet<File>()
            getSmaliFilesInDir(File(srcDirectory), filesToProcessSet)
            var errors = false
            val dexBuilder = DexBuilder(Opcodes.forApi(15))
            val executor = Executors.newFixedThreadPool(options.jobs)
            val tasks: MutableList<Future<Boolean>> = Lists.newArrayList()
            for (file in filesToProcessSet) {
                tasks.add(executor.submit<Boolean> { assembleSmaliFile(file, dexBuilder, options) })
            }
            for (task in tasks) {
                while (true) {
                    try {
                        if (!task.get()) {
                            errors = true
                        }
                    } catch (ex: InterruptedException) {
                        continue
                    }
                    break
                }
            }
            executor.shutdown()
            if (errors) {
                throw Exception("Encountered errors while compiling smali files.")
            }
            dexBuilder.writeTo(FileDataStore(File(options.outputDexFile)))
        } catch (ex: RuntimeException) {
            ex.printStackTrace()
            throw Exception("UNEXPECTED TOP-LEVEL EXCEPTION: " + ex.message)
        } catch (ex: Throwable) {
            ex.printStackTrace()
            throw Exception("UNEXPECTED TOP-LEVEL ERROR: " + ex.message)
        }
    }

    private fun getSmaliFilesInDir(dir: File, smaliFiles: MutableSet<File>) {
        val files = dir.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    getSmaliFilesInDir(file, smaliFiles)
                } else if (file.name.endsWith(".smali")) {
                    smaliFiles.add(file)
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun assembleSmaliFile(smaliFile: File, dexBuilder: DexBuilder, options: SmaliOptions): Boolean {
        FileInputStream(smaliFile).use { fis ->
            val reader = InputStreamReader(fis, StandardCharsets.UTF_8)
            val lexer = smaliFlexLexer(reader, options.apiLevel)
            lexer.setSourceFile(smaliFile)
            val tokens = CommonTokenStream(lexer)
            val parser = smaliParser(tokens)
            parser.setVerboseErrors(options.verboseErrors)
            parser.setAllowOdex(options.allowOdexOpcodes)
            parser.setApiLevel(options.apiLevel)
            val result = parser.smali_file()
            if (parser.numberOfSyntaxErrors > 0 || lexer.numberOfSyntaxErrors > 0) {
                val sourceName = lexer.sourceName
                val errorMsg = "Source: " + sourceName + " Line: " + lexer.line + " Column: " + lexer.column
                if (sourceName == "") {
                    throw Exception("Error occurred while compiling " + smaliFile.name)
                } else {
                    throw Exception(errorMsg)
                }
            }
            val t = result.tree
            val treeStream = CommonTreeNodeStream(t)
            treeStream.tokenStream = tokens
            if (options.printTokens) {
                println(t.toStringTree())
            }

            val dexGen = smaliTreeWalker(treeStream)
            dexGen.setApiLevel(options.apiLevel)
            dexGen.setVerboseErrors(options.verboseErrors)
            dexGen.setDexBuilder(dexBuilder)
            dexGen.smali_file()
            if (dexGen.numberOfSyntaxErrors != 0) {
                val sourceName = lexer.sourceName
                val errorMsg = "Source: " + sourceName + " Line: " + lexer.line + " Column: " + lexer.column
                if (sourceName == "") {
                    throw Exception("Error occurred while compiling " + smaliFile.name)
                } else {
                    throw Exception(errorMsg)
                }
            }
            return true
        }
    }
}