package com.mcal.apkeditor.dialogs

import android.app.Activity
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.BuildConfig
import com.mcal.apkeditor.R

// TODO REMOVE ME
class EditModeView(
    private val mActivity: Activity,
    private val mCallback: IEditModeSelected,
    private val mApkPath: String
) {
    fun showFileEditDialog() {
        val context = mActivity
        val dialog = MaterialAlertDialogBuilder(context)
        dialog.setItems(
            arrayOf(
                context.getString(R.string.full_edit),
                context.getString(R.string.simple_edit),
                context.getString(R.string.common_edit),
                context.getString(R.string.xml_file_edit),
                context.getString(R.string.sign_apk)
            )
        ) { p112: DialogInterface, p2: Int ->
            when (p2) {
                0 -> {
                    mCallback.editModeSelected(FULL_EDIT, mApkPath)
                    p112.dismiss()
                }
                1 -> {
                    mCallback.editModeSelected(SIMPLE_EDIT, mApkPath)
                    p112.dismiss()
                }
                2 -> {
                    mCallback.editModeSelected(COMMON_EDIT, mApkPath)
                    p112.dismiss()
                }
                3 -> {
                    if (BuildConfig.IS_PRO) {
                        mCallback.editModeSelected(XML_FILE_EDIT, mApkPath)
                    }
                    p112.dismiss()
                }

            }
        }
        dialog.create().show()
    }

    interface IEditModeSelected {
        fun editModeSelected(mode: Int, filePath: String?)
        fun updateFileList(path: String)
    }

    companion object {
        const val FULL_EDIT = 0
        const val SIMPLE_EDIT = 1
        const val COMMON_EDIT = 2
        const val XML_FILE_EDIT = 4
    }
}