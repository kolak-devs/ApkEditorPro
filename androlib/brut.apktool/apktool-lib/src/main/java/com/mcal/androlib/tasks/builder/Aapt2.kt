package com.mcal.androlib.tasks.builder

import brut.util.OS
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.ScopedStorage.getBinDir
import com.mcal.common.utils.createNewFile
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
        try {
            val args: MutableList<String> = ArrayList()
            args.add(ScopedStorage.getAapt2().path)
            args.add("compile")
            args.add("--dir")
            args.add(resDir.absolutePath)
            args.add("-o")
            args.add(createNewFile(buildDir, "resources.zip").absolutePath)
            OS.exec(args.toTypedArray())
        } catch (ex: Exception) {
            ex.printStackTrace()
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
        try {
            val args: MutableList<String> = ArrayList()
            args.add(getBinDir().toString() + File.separator + "aapt2")
            args.add("link")
            args.add("--allow-reserved-package-id")
            args.add("--no-version-vectors")
            args.add("--no-version-transitions")
            args.add("--auto-add-overlay")
            args.add("--min-sdk-version")
            args.add(minSdk ?: "21")
            args.add("--target-sdk-version")
            args.add(targetSdk ?: "32")

            include?.forEach { framework ->
                args.add("-I");
                args.add(framework.path);
            }

            //add compiled resources
            val projectZip = File(buildDir, "resources.zip")
            if (projectZip.exists()) {
                args.add("-R")
                args.add(projectZip.absolutePath)
            }

            args.add("--manifest")
            args.add(manifest.absolutePath)

            args.add("-o")
            if (!apkFile.exists()) {
                apkFile.createNewFile()
            }
            args.add(apkFile.path)

            OS.exec(args.toTypedArray())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}