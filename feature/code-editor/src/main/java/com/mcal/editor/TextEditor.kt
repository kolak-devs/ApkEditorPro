package com.mcal.editor

import android.content.Context
import android.content.Intent
import com.mcal.common.utils.ActivityHelper
import com.mcal.editor.presentation.EditorActivity

object TextEditor {
    @JvmStatic
    fun getSoraEditor(
        context: Context,
        filepath: String,
        apkPath: String?,
        startLine: Int,
        searchString: String?
    ): Intent {
        val intent = Intent(context, EditorActivity::class.java)
        ActivityHelper.attachParam(intent, "filePath", filepath)
        apkPath?.let { ActivityHelper.attachParam(intent, "apkPath", apkPath) }
        ActivityHelper.attachParam(intent, "startLine", startLine)
        searchString?.let { ActivityHelper.attachParam(intent, "searchString", searchString) } // todo
        return intent
    }

    @JvmStatic
    fun getSoraEditor(
        context: Context,
        filePathList: ArrayList<String?>?,
        index: Int,
        apkPath: String?,
        startLineList: ArrayList<Int>?,
        searchString: String?
    ): Intent {
        val intent = Intent(context, EditorActivity::class.java)
        filePathList?.let { intent.putStringArrayListExtra("fileList", filePathList) }
        intent.putExtra("curFileIndex", index)
        apkPath?.let { intent.putExtra("apkPath", apkPath) }
        startLineList?.let { intent.putIntegerArrayListExtra("startLineList", startLineList) }
        searchString?.let { intent.putExtra("searchString", searchString) } // todo
        return intent
    }

    @JvmStatic
    fun getSoraEditor(
        context: Context,
        tmpFilePath: String,
        filePath: String,
        isRootMode: Boolean
    ): Intent {
        val intent = Intent(context, EditorActivity::class.java)
        ActivityHelper.attachParam(intent, "filePath", tmpFilePath)
        ActivityHelper.attachParam(intent, "realFilePath", filePath)
        ActivityHelper.attachParam(intent, "isRootMode", isRootMode)
        return intent
    }
}