package com.mcal.editor.dialogs

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.common.utils.ScopedStorage
import com.mcal.editor.TextEditor
import com.mcal.editor.utils.JavaExtractor
import com.mcal.neweditor.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DexToJava(private val context: Context, private val filePath: File, private val apkPath: File) {
    fun show() {
        val tmpDir = ScopedStorage.getTmpDir().path

        val dexAndClass = getDexAndClassName()
        val dexName = dexAndClass[0]
        val className = dexAndClass[1]

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_proccessing, null)

        val dialog = MaterialAlertDialogBuilder(context).create()
        dialog.setView(view)
        dialog.setCancelable(false)
        dialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            val extractor = JavaExtractor(apkPath.path, dexName, className, tmpDir)
            val succeed = extractor.extract()
            var errMessage: String? = null
            if (!succeed) {
                errMessage = extractor.errorMessage
            }

            withContext(Dispatchers.Main) {
                if (succeed) {
                    var relativePath = className.substring(1)
                    var filePath = "$tmpDir/$relativePath.java"
                    var fileExist = File(filePath).exists()
                    if (!fileExist) {
                        do {
                            // Try to remove string after $
                            val position = relativePath.lastIndexOf('$')
                            if (position != -1) {
                                relativePath = relativePath.substring(0, position)
                                filePath = "$tmpDir/$relativePath.java"
                                fileExist = File(filePath).exists()
                                if (fileExist) {
                                    break
                                }
                            }

                            // Try to get the file in defpackage folder
                            filePath = tmpDir + File.separator + "defpackage/" + relativePath + ".java"
                            fileExist = File(filePath).exists()
                            if (fileExist) {
                                break
                            }
                        } while (false)
                    }
                    if (!fileExist) {
                        Toast.makeText(
                            context,
                            "Cannot find java file",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val intent: Intent =
                            TextEditor.getSoraEditor(
                                context,
                                filePath,
                                null,
                                0,
                                null
                            )
                        context.startActivity(intent)
                    }
                } else {
                    Toast.makeText(context, errMessage, Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
        }
    }

    private fun getDexAndClassName(): Array<String> {
        var dexName = "classes.dex"
        val className = StringBuilder()
        val dirs = filePath.path.split("/").toTypedArray()

        var i = 0
        while (i < dirs.size) {
            if ("smali" == dirs[i]) {
                break
            }
            if (dirs[i].startsWith("smali_")) {
                dexName = dirs[i].substring(6) + ".dex"
                break
            }
            i++
        }

        className.append('L')
        i += 1
        while (i < dirs.size) {
            var name = dirs[i]
            if (i == dirs.size - 1) {
                if (name.length > 6 && name.endsWith(".smali")) {
                    name = name.substring(0, name.length - 6)
                    className.append(name)
                }
            } else {
                className.append(name)
                className.append('/')
            }
            i++
        }
        return if (className.isEmpty()) emptyArray() else arrayOf(dexName, className.toString())
    }
}