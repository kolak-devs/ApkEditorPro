package com.mcal.apkeditor.dialogs

import android.app.Activity
import android.content.DialogInterface
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.BuildConfig
import com.mcal.apkeditor.R
import com.mcal.apksigner.ApkSigner
import com.mcal.common.view.ProgressDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class EditModeView(
    private val mActivity: Activity,
    private val mCallback: IEditModeSelected,
    private val mApkPath: String,
    private val mPackageName: String?
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
                4 -> {
                    sign(mApkPath, mApkPath.replace(".apk", "_sign.apk"))
                    p112.dismiss()
                }
            }
        }
        dialog.create().show()
    }

    fun sign(unsignedPath: String, signedPath: String) {
        ProgressDialog(
            mActivity, "Signing", "Please wait...", false,
            object : ProgressDialog.ProcessingInterface {
                @Throws(Exception::class)
                override fun process() {
                    CoroutineScope(Dispatchers.IO).launch {
                        ApkSigner().signApk(unsignedPath, signedPath)
                    }
                }

                override fun afterProcess() {
                    mCallback.updateFileList(signedPath)
                    Toast.makeText(mActivity, "Apk signed success", Toast.LENGTH_SHORT).show()
                }
            }, -1
        ).show()
    }

    fun showAppEditDialog() {
        val context = mActivity
        val dialog = MaterialAlertDialogBuilder(context)
        dialog.setItems(
            arrayOf(
                context.getString(R.string.full_edit),
                context.getString(R.string.simple_edit),
                context.getString(R.string.common_edit),
                context.getString(R.string.xml_file_edit),
                context.getString(R.string.edit_data_root)
            )
        ) { _: DialogInterface?, p2: Int ->
            val apkPath = mApkPath
            when (p2) {
                0 -> mCallback.editModeSelected(FULL_EDIT, apkPath)
                1 -> mCallback.editModeSelected(SIMPLE_EDIT, apkPath)
                2 -> mCallback.editModeSelected(COMMON_EDIT, apkPath)
                3 -> if (BuildConfig.IS_PRO) {
                    mCallback.editModeSelected(XML_FILE_EDIT, apkPath)
                }
                4 -> mCallback.editModeSelected(DATA_EDIT, mPackageName)
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
        const val DATA_EDIT = 3
        const val XML_FILE_EDIT = 4
    }
}