package com.mcal.bshengine.api

import jadx.api.JadxDecompiler
import jadx.plugins.input.smali.SmaliInputPlugin
import java.nio.file.Path
import java.nio.file.Paths

class XSmali {
    fun smali2java(smaliFilePath: String): String? {
        JadxDecompiler().use { decompiler ->
            decompiler.addCustomLoad(
                SmaliInputPlugin().loadFiles(
                    listOf<Path>(Paths.get(smaliFilePath))
                )
            )
            decompiler.load()
            for (cls in decompiler.classes) {
                return cls.code
            }
        }
        return null
    }
}