package com.mcal.bshengine.api

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class XAlert(activity: Activity) {
    private val dialog = MaterialAlertDialogBuilder(activity)

    /**
     * Since: 2.4.6
     */
    fun show(title: String, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            dialog.apply {
                setTitle(title)
                setMessage(message)
                setPositiveButton("Ok", null)
            }.show()
        }
    }

    /**
     * Since: 2.4.7
     */
    fun setTitle(title: String): XAlert {
        dialog.setTitle(title)
        return this
    }

    /**
     * Since: 2.4.7
     */
    fun setMessage(title: String): XAlert {
        dialog.setMessage(title)
        return this
    }

    /**
     * Since: 2.4.7
     */
    fun setPositiveButton(text: String, clickListener: XAlertClickListener): XAlert {
        dialog.setPositiveButton(text) { _, _ ->
            clickListener.onPositive()
        }
        return this
    }

    /**
     * Since: 2.4.7
     */
    fun setNegativeButton(text: String, clickListener: XAlertClickListener): XAlert {
        dialog.setPositiveButton(text) { _, _ ->
            clickListener.onNegative()
        }
        return this
    }

    /**
     * Since: 2.4.7
     */
    fun setNeutralButton(text: String, clickListener: XAlertClickListener): XAlert {
        dialog.setPositiveButton(text) { _, _ ->
            clickListener.onNeutral()
        }
        return this
    }

    /**
     * Since: 2.4.7
     */
    fun show() {
        dialog.show()
    }
}

