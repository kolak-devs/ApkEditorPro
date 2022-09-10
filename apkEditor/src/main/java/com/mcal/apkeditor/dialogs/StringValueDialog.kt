package com.mcal.apkeditor.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mcal.apkeditor.R
import com.mcal.apkeditor.StringListAdapter
import com.mcal.common.utils.ClipboardUtils

class StringValueDialog @SuppressLint("InflateParams") constructor(
    context: Context,
    strListAdapter: StringListAdapter,
    position: Int
) {
    private val mKey: TextInputLayout
    private val mValue: TextInputEditText

    fun setKeyValue(key: String?, value: String?) {
        mKey.hint = key
        mValue.setText(value)
        mValue.setSelection(value?.length ?: 0)
    }

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_stringvalue, null)
        val menu = view.findViewById<ImageButton>(R.id.menu_clipboard)
        mKey = view.findViewById(R.id.key)
        mValue = view.findViewById(R.id.value)
        menu.setOnClickListener {
            val str = mKey.hint.toString()
            ClipboardUtils.copyToClipboard(context, str)
            val msg = String.format(context.getString(R.string.copied_to_clipboard), str)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
        val materialDialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .create()
        materialDialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            context.getString(android.R.string.ok)
        ) { dialog: DialogInterface, _: Int ->
            val newValue = mValue.text.toString()
            strListAdapter.checkTextChange(position, newValue)
            dialog.dismiss()
        }
        materialDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            context.getString(android.R.string.cancel)
        ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        materialDialog.show()
    }
}