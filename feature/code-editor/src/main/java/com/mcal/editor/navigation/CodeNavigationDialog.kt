package com.mcal.editor.navigation

import android.app.Activity
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.neweditor.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.regex.Matcher
import java.util.regex.Pattern

// Popup window helper
class CodeNavigationDialog(private val mCallback: ISmaliMethodClicked) : CodeNavigationClick {
    private lateinit var mMethodList: List<CodeNavigationInfo>
    private lateinit var mMaterialDialog: AlertDialog

    fun asyncShowPopup(activity: Activity, filePath: String, text: String) {
        val methodList: MutableList<CodeNavigationInfo> = ArrayList()
        val br = BufferedReader(StringReader(text))
        var line: String?
        try {
            CoroutineScope(Dispatchers.IO).launch {
                launch(Dispatchers.IO) {
                    var lineIndex = 0
                    if (filePath.endsWith(".smali")) {
                        while (br.readLine().also { line = it } != null) {
                            val mLine = line
                            if (mLine != null) {
                                if (mLine.startsWith(".method ")) {
                                    val prototype = mLine.substring(8)
                                    methodList.add(CodeNavigationInfo(lineIndex, prototype))
                                } else if (mLine.startsWith(".field ")) {
                                    val prototype = mLine.substring(7)
                                    methodList.add(CodeNavigationInfo(lineIndex, prototype))
                                }
                            }
                            lineIndex += 1
                        }
                    } else if (filePath.endsWith(".java")) {
                        var matcher: Matcher
                        var mLine: String?
                        while (br.readLine().also { line = it } != null) {
                            mLine = line
                            if (mLine != null) {
                                mLine = mLine.trim()
                                if (mLine.length > 6 && mLine[0] == 'p' && (mLine[1] == 'u' || mLine[1] == 'r')) {
                                    matcher = Pattern.compile(
                                        "^[ \\t]*(?:(?:public|protected|private)\\s+)?(?:(static|final|native|synchronized|abstract|threadsafe|transient|<[?\\w\\[\\] ,&]+>|<[^<]*<[?\\w\\[\\] ,&]+>[^>]*>|<[^<]*<[^<]*<[?\\w\\[\\] ,&]+>[^>]*>[^>]*>)\\s+)*(?!return)\\b([\\w.]+)\\b(?:|<[?\\w\\[\\] ,&]+>|<[^<]*<[?\\w\\[\\] ,&]+>[^>]*>|<[^<]*<[^<]*<[?\\w\\[\\] ,&]+>[^>]*>[^>]*>)((?:\\[])*)\\s+\\b\\w+\\b\\s*\\(\\s*(?:\\b([\\w.]+)\\b(?:|<[?\\w\\[\\] ,&]+>|<[^<]*<[?\\w\\[\\] ,&]+>[^>]*>|<[^<]*<[^<]*<[?\\w\\[\\] ,&]+>[^>]*>[^>]*>)((?:\\[])*)(\\.\\.\\.)?\\s+(\\w+)\\b(?![>\\[])\\s*(?:,\\s+\\b([\\w.]+)\\b(?:|<[?\\w\\[\\] ,&]+>|<[^<]*<[?\\w\\[\\] ,&]+>[^>]*>|<[^<]*<[^<]*<[?\\w\\[\\] ,&]+>[^>]*>[^>]*>)((?:\\[])*)(\\.\\.\\.)?\\s+(\\w+)\\b(?![>\\[])\\s*)*)?\\s*\\)(?:\\s*throws [\\w.]+(\\s*,\\s*[\\w.]+))?\\s*[{;][ \\t]*$"
                                    ).matcher(mLine)
                                    if (matcher.matches()) {
                                        var prototype = matcher.group(0)
                                        if (prototype != null && (prototype.endsWith("{") || prototype.endsWith(";"))) {
                                            prototype = prototype.substring(0, prototype.length - 1)
                                            methodList.add(CodeNavigationInfo(lineIndex, prototype.trim()))
                                        }
                                    } else {
                                        matcher = Pattern.compile(
                                            "\\s*(?:private|public|protected)?\\s*(?:static)?\\s*(?:final)?\\s*([\\w<>\\[\\] ?.]+|[\\w<>\\[\\], ?.]+)\\s+([\\w, +]+)\\s*=?\\s*(?:new)?\\s*(?:[\\w.]+)?\\s*\\(?\\s*;?"
                                        ).matcher(mLine)
                                        if (matcher.matches()) {
                                            var fieldName = matcher.group(2)
                                            if (fieldName != null) {
                                                val fieldType = matcher.group(1)
                                                if (fieldType != null) {
                                                    fieldName = "$fieldType $fieldName"
                                                }
                                                methodList.add(CodeNavigationInfo(lineIndex, fieldName.trim()))
                                            }
                                        }
                                    }
                                }
                            }
                            lineIndex += 1
                        }
                    }
                    if (methodList.isNotEmpty()) {
                        mMethodList = methodList
                        withContext(Dispatchers.Main) {
                            val layout = LayoutInflater.from(activity).inflate(R.layout.dialog_methods_list, null).apply {
                                findViewById<RecyclerView>(R.id.methods).apply {
                                    layoutManager = LinearLayoutManager(activity)
                                    adapter = CodeNavigationAdapter(this@CodeNavigationDialog, methodList)
                                }
                            }
                            val materialDialog = MaterialAlertDialogBuilder(activity).create()
                            mMaterialDialog = materialDialog
                            materialDialog.setView(layout)
                            materialDialog.show()
                        }
                    }
                }

            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onClick(position: Int) {
        val methodList = mMethodList
        if (position < methodList.size) {
            val info = methodList[position]
            mCallback.gotoLine(info.lineIndex)
            mMaterialDialog.dismiss()
        }
    }

    interface ISmaliMethodClicked {
        fun gotoLine(lineNO: Int)
    }
}