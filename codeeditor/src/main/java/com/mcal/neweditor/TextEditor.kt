package com.mcal.neweditor

import android.content.Context
import android.content.Intent
import com.mcal.common.utilsOld.ActivityUtils
import com.mcal.neweditor.editor2.EditorActivity

object TextEditor {
    @JvmStatic
    fun getSoraEditor(context: Context,
                      filepath: String,
                      apkPath: String?,
                      startLine: Int,
                      searchString: String?
    ): Intent {
        val intent = Intent(context, EditorActivity::class.java)
        ActivityUtils.attachParam(intent, "filePath", filepath)
        apkPath?.let { ActivityUtils.attachParam(intent, "apkPath", apkPath) }
        ActivityUtils.attachParam(intent, "startLine", startLine)
        searchString?.let { ActivityUtils.attachParam(intent, "searchString", searchString) } // todo
        // extraString?.let { ActivityUtils.attachParam(intent, "extraString", extraString) } // todo delete
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
        //Toast.makeText(context, startLineList!![index].toString(), Toast.LENGTH_LONG).show()
        return intent
    }

    @JvmStatic
    fun getSoraEditor(
        context: Context,
        tmpFilePath: String,
        filePath: String,
        isRootMode: Boolean,
        ints: IntArray?
    ): Intent {
        val intent = Intent(context, EditorActivity::class.java)
        ActivityUtils.attachParam(intent, "filePath", tmpFilePath)
        ActivityUtils.attachParam(intent, "realFilePath", filePath)
        ActivityUtils.attachParam(intent, "isRootMode", isRootMode)
        ints?.let { ActivityUtils.attachParam(intent, "resourceIds", ints) }
        return intent
    }
}