package com.mcal.androlib.tasks.builder

import brut.util.OS
import com.mcal.common.utils.ScopedStorage.getAapt
import java.io.File

object Aapt {
    @JvmStatic
    fun build(
        apkFile: File,
        include: Array<File?>?,
        manifest: File,
        resDir: File,
        minSdk: String?,
        targetSdk: String?
    ) {
        val args: MutableList<String> = ArrayList()
        args.add(getAapt().path)
        args.add("package")
        args.add("-f")
        include?.forEach { framework ->
            framework?.let {
                args.add("-I");
                args.add(it.path)
            };
        }

        args.add("--min-sdk-version")
        args.add(minSdk ?: "21")

        args.add("--target-sdk-version")
        args.add(targetSdk ?: "32")

        args.add("-S")
        args.add(resDir.absolutePath)
        args.add("-M")
        args.add(manifest.path)
        args.add("-F")
        if (!apkFile.exists()) {
            apkFile.createNewFile()
        }
        args.add(apkFile.path)

        OS.exec(args.toTypedArray())
    }
}