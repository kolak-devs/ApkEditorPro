package com.mcal.apkeditor

import com.mcal.apkeditor.ce.IApkMaking
import com.mcal.common.utils.ITaskCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

abstract class ComposeThread : CoroutineScope {
    abstract fun stopRunning()
    abstract fun setExtraMaker(extraMaker: IApkMaking?)
    abstract fun setModification(
        strModified: Boolean,
        manifestModified: Boolean,
        resFileModified: Boolean,
        modifiedSmaliFolders: List<String>?,
        addedFiles: MutableMap<String, String>?,
        replacedFiles: MutableMap<String, String>?,
        deletedFiles: MutableSet<String>?,
        fileEntry2ZipEntry: Map<String, String>?,
        bSignApk: Boolean
    )

    abstract fun setTaskCallback(callback: ITaskCallback?)

    abstract fun execute(): Job
}