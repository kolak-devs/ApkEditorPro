package com.mcal.apkeditor.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.R
import com.mcal.apkeditor.activities.MainActivity
import java.lang.ref.WeakReference
import java.util.*

class AppAgreementDialog @SuppressLint("SetTextI18n") constructor(activity: MainActivity) {
    private val activityRef: WeakReference<MainActivity>

    companion object {
        @JvmStatic
        fun appLicenseAccepted(ctx: Context?): Boolean {
            val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx!!)
            return sp.getBoolean("app_agreement_accepted", false)
        }
    }

    init {
        activityRef = WeakReference(activity)
        val inflater: LayoutInflater = LayoutInflater.from(activity)
        val layout: View = inflater.inflate(R.layout.dlg_app_license, null)
        val tv: TextView = layout.findViewById<View>(R.id.tv_content) as TextView
        tv.text = """
        Information in this dialog is provided in connection with APK Editor. No license, express or implied, by estoppel or otherwise, to any intellectual property rights is granted by this.
        
        APK Editor is designed for Android fans who know what exactly they are doing, but not intended for hack, please use it under following terms:
        
        1) Please only modify the apk files which you have intellectual property rights.
        
        2) For apk files you don't have intellectual property rights, you need to ask for authorities from the developer to modify it. And even though you have rights to modify it, you still need to ask for re-distribution rights to publish it.
        
        3) To prevent abuse of APK Editor, sign feature is not provided any more.
        
        4) We may make changes to specifications and product descriptions at any time, without notice.
        """.trimIndent()
        val inputEt: EditText = layout.findViewById<View>(R.id.et_input) as EditText
        val dialog = MaterialAlertDialogBuilder(activity)
        dialog.setTitle("Agreement")
        dialog.setView(layout)
        dialog.setCancelable(false)
        dialog.setPositiveButton(android.R.string.ok) { v, _ ->
            val input: String = inputEt.text.toString()
            if (input.trim { it <= ' ' }.lowercase(Locale.getDefault()) == "accept") {
                activityRef.get()!!.initFileWithPermissionCheck()
                val sp: SharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(activityRef.get()!!)
                val e: SharedPreferences.Editor = sp.edit()
                e.putBoolean("app_agreement_accepted", true)
                e.apply()
                v.dismiss()
            } else {
                Toast.makeText(activityRef.get(), R.string.input_agree_toast, Toast.LENGTH_SHORT)
                    .show()
                activityRef.get()!!.finish()
            }
        }
        dialog.setNegativeButton(android.R.string.cancel) { _, _ -> activityRef.get()!!.finish() }
        dialog.show()
    }
}