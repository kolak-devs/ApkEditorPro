package com.mcal.common.view

import android.app.Activity
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.common.databinding.DialogProgressSimpleBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProgressDialog(
    private val mActivity: Activity,
    private val title: String,
    private val message: String,
    private val cancelable: Boolean = false,
    private val mProcessor: ProcessingInterface,
    private val successTipResId: Int
) {
    private lateinit var viewBinding: DialogProgressSimpleBinding
    private lateinit var materialDialog: AlertDialog

    fun create(): ProgressDialog {
        viewBinding = DialogProgressSimpleBinding.inflate(LayoutInflater.from(mActivity))
        viewBinding.message.text = message
        materialDialog = MaterialAlertDialogBuilder(mActivity)
            .setTitle(title)
            .setCancelable(cancelable)
            .setView(viewBinding.root)
            .create()
        return this
    }

    fun show(): ProgressDialog {
        create()
        runProcess()
        materialDialog.show()
        return this
    }

    fun dismiss() {
        materialDialog.dismiss()
    }

    fun setMessage(message: String) {
        viewBinding.message.text = message
    }

    private fun showTip(resId: Int) {
        if (resId != -1) {
            val activity = mActivity
            Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show()
        }
    }

    interface ProcessingInterface {
        @Throws(Exception::class)
        fun process()
        fun afterProcess()
    }

    private fun runProcess() {
        CoroutineScope(Dispatchers.IO).launch {
            mProcessor.process()
            withContext(Dispatchers.Main) {
                mActivity.runOnUiThread {
                    mProcessor.afterProcess()
                    showTip(successTipResId)
                    if (materialDialog.isShowing) {
                        materialDialog.dismiss()
                    }
                }
            }
        }
    }
}
