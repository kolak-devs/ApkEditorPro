package com.mcal.common.tasks

import com.mcal.common.tasks.LogHelper.formatLog
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.ScopedStorage.getBinDir
import com.mcal.common.utils.createNewFile
import com.mcal.common.utils.readInputStream
import java.io.File


object Aapt2 {
    @JvmStatic
    fun build(
        apkFile: File,
        include: Array<File>,
        manifest: File,
        resDir: File,
        minSdk: String?,
        targetSdk: String?
    ) {
        val buildDir = File(resDir.parent, "build")
        compile(resDir, buildDir)
        link(apkFile, buildDir, include, manifest, minSdk, targetSdk)
    }

    private fun compile(
        resDir: File,
        buildDir: File
    ) {
        val args: MutableList<String> = ArrayList()
        args.add(ScopedStorage.getAapt2().path)
        args.add("compile")
        args.add("--dir")
        args.add(resDir.absolutePath)
        args.add("-o")
        args.add(createNewFile(buildDir, resDir.name + ".zip").absolutePath)

        val aaptProcess = Runtime.getRuntime().exec(args.toTypedArray())
        val error = aaptProcess.errorStream.readInputStream()
        if (error.isNotEmpty()) {
            throw Exception(formatLog(error))
        }

        resDir.parent?.let { path ->
            compileLibraries(File(path), buildDir)
        }
    }

    private fun compileLibraries(
        path: File,
        buildDir: File
    ) {
        path.listFiles()?.let { resources ->
            for (resDir in resources) {
                if (resDir.name.startsWith("res_")) {
                    if (!resDir.exists() || !resDir.isDirectory) {
                        continue
                    }
                    val args: MutableList<String> = ArrayList()
                    args.add(ScopedStorage.getAapt2().path)
                    args.add("compile")
                    args.add("--dir")
                    args.add(resDir.absolutePath)
                    args.add("-o")
                    args.add(createNewFile(buildDir, resDir.name + ".zip").absolutePath)

                    val aaptProcess = Runtime.getRuntime().exec(args.toTypedArray())
                    val error = aaptProcess.errorStream.readInputStream()
                    if (error.isNotEmpty()) {
                        throw Exception(formatLog(error))
                    }
                }
            }
        }
    }

    private fun link(
        apkFile: File,
        buildDir: File,
        include: Array<File>?,
        manifest: File,
        minSdk: String?,
        targetSdk: String?
    ) {
        val args: MutableList<String> = ArrayList()
        args.add(getBinDir().toString() + File.separator + "aapt2")
        args.add("link")
        include?.forEach { framework ->
            args.add("-I")
            args.add(framework.path)
        }
        args.add("--allow-reserved-package-id")
        args.add("--no-version-vectors")
        args.add("--no-version-transitions")
        args.add("--auto-add-overlay")
        args.add("--min-sdk-version")
        args.add(minSdk ?: "21")
        args.add("--target-sdk-version")
        args.add(targetSdk ?: "32")
        buildDir.listFiles()?.let { resources ->
            for (resource in resources) {
                if (resource.isDirectory) {
                    continue
                }
                if (!resource.name.endsWith(".zip")) {
                    continue
                }
                args.add("-R")
                args.add(resource.absolutePath)
            }
        }

        args.add("--manifest")
        args.add(manifest.absolutePath)

        args.add("-o")
        if (!apkFile.exists()) {
            apkFile.createNewFile()
        }
        args.add(apkFile.path)

        val aaptProcess = Runtime.getRuntime().exec(args.toTypedArray())
        val error = aaptProcess.errorStream.readInputStream()
        if (error.isNotEmpty()) {
            throw Exception(formatLog(error))
        }
    }
}