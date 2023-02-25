package com.mcal.bshengine.api

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class XAlert(private val activity: Activity) {
    /**
     * Since: 2.4.6
     */
    fun show(title: String, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            MaterialAlertDialogBuilder(activity).apply {
                setTitle(title)
                setMessage(message)
                setPositiveButton("Ok", null)
            }.show()
        }
    }
}