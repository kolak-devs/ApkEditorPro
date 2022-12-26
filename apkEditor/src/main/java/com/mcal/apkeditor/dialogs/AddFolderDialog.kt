package com.mcal.apkeditor.dialogs

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.mcal.apkeditor.R

class AddFolderDialog(context: Context, callback: AddFolderCallback) {
    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_folder, null)
        val textView = view.findViewById<TextInputEditText>(R.id.file_name)
        val materialDialog = MaterialAlertDialogBuilder(context)
        materialDialog.setView(view)
        materialDialog.setPositiveButton(R.string.folder) { dialog: DialogInterface, _: Int ->
            val text = textView.text.toString()
            if (text.isNotEmpty()) {
                callback.createFolder(text)
                dialog.dismiss()
            }
        }
        materialDialog.setNegativeButton(R.string.file) { dialog: DialogInterface, _: Int ->
            val text = textView.text.toString()
            if (text.isNotEmpty()) {
                callback.createFile(text)
                dialog.dismiss()
            }
        }
        materialDialog.setNeutralButton(R.string.import_file) { _: DialogInterface?, _: Int -> callback.importFile() }
        materialDialog.show()
    }

    interface AddFolderCallback {
        fun createFolder(folderName: String?)
        fun createFile(folderName: String?)
        fun importFile()
    }
}