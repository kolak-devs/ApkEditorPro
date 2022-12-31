package com.mcal.editor.dialogs

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.common.utils.ScopedStorage
import com.mcal.editor.TextEditor
import com.mcal.editor.utils.FileUtils
import com.mcal.neweditor.R
import jadx.api.JadxDecompiler
import jadx.plugins.input.smali.SmaliInputPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class SmaliToJava(private val context: Context, private val filePath: File) {
    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_proccessing, null)
        val dialog = MaterialAlertDialogBuilder(context).create()
        dialog.setView(view)
        dialog.setCancelable(false)
        dialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            JadxDecompiler().use { decompiler ->
                decompiler.addCustomLoad(
                    SmaliInputPlugin().loadFiles(
                        listOf<Path>(
                            Paths.get(
                                filePath.path
                            )
                        )
                    )
                )
                decompiler.load()
                for (cls in decompiler.classes) {
                    // Создаём временную директорию пакета для записи Java класса
                    val tmpDir = File(ScopedStorage.getTmpDir(), cls.getPackage().replace(".", "/"))
                    if (!tmpDir.exists()) {
                        tmpDir.mkdirs()
                    }
                    // Полный путь к Java классу
                    val javaPath = tmpDir.toString() + File.separator + cls.name + ".java"
                    // Записываем декодированный контент в Java класс
                    FileUtils.writeText(javaPath, cls.code)
                    withContext(Dispatchers.Main) {
                        val intent = TextEditor.getSoraEditor(context, javaPath, null, 0, null)
                        context.startActivity(intent)
                        dialog.dismiss()
                    }
                }
            }
        }
    }
}