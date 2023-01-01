package com.mcal.apkeditor.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.mcal.apkeditor.R
import com.mcal.apkeditor.activities.MainActivity
import java.util.*

class AppAgreementDialog @SuppressLint("SetTextI18n") constructor(activity: MainActivity) {

    companion object {
        @JvmStatic
        fun appLicenseAccepted(ctx: Context?): Boolean {
            val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx!!)
            return sp.getBoolean("app_agreement_accepted", false)
        }
    }

    init {
        val layout = LayoutInflater.from(activity).inflate(R.layout.dialog_app_license, null)
        val textView = layout.findViewById<TextView>(R.id.tv_content)
        textView.text = if (Locale.getDefault().language.contains("ru")) buildString {
            append("Данная информация относится к приложению ApkEditor Pro.")
            append("\n\n")
            append("APK Editor Pro ApkEditor - приложение для анализа мобильных приложений с возможностью вносить правки в коде, а также тестировать приложение на уязвимости. Он не предназначен для взлома приложений! Пожалуйста используйте его на следующих условиях:")
            append("\n\n")
            append("1) Пожалуйста, изменяйте только те файлы APK, на которые у вас есть права интеллектуальной собственности.")
            append("\n\n")
            append("2) Для APK файлов, на которые у вас нет прав интеллектуальной собственности, вам необходимо запросить полномочия у разработчика, чтобы изменить их. И хотя у вас есть права на его изменение, вам все равно нужно запрашивать права на повторное распространение для его публикации.")
            append("\n\n")
            append("3) Мы можем вносить изменения в спецификации и описания продуктов в любое время без предварительного уведомления.")
            append("\n\n")
            append("Если вы не согласны с выше перечисленными пунктами, пожалуйста удалите приложение и не используйте его.")
        } else buildString {
            append("This information applies to the ApkEditor Pro application.")
            append("\n\n")
            append("APK Editor Pro ApkEditor is an application for analyzing mobile applications with the ability to make changes in the code, as well as test the application for vulnerabilities. It is not designed to hack apps! Please use it under the following conditions:")
            append("\n\n")
            append("1) Please only modify APK files for which you have intellectual property rights.")
            append("\n\n")
            append("2) For APK files for which you do not have intellectual property rights, you need to request permission from the developer to change them. And while you have rights to modify it, you still need to request redistribution rights to publish it.")
            append("\n\n")
            append("3) We may make changes to product specifications and descriptions at any time without notice.")
            append("\n\n")
            append("If you do not agree with the above points, please delete the application and do not use it.")
        }

        val textInputView = layout.findViewById<TextInputEditText>(R.id.et_input)
        val dialog = MaterialAlertDialogBuilder(activity)
        dialog.setTitle(activity.getString(R.string.agreement))
        dialog.setView(layout)
        dialog.setCancelable(false)
        dialog.setPositiveButton(android.R.string.ok) { v, _ ->
            val input = textInputView.text.toString()
            if (input.trim().lowercase(Locale.getDefault()) == "accept") {
                activity.initFileWithPermissionCheck()
                PreferenceManager.getDefaultSharedPreferences(activity).edit().putBoolean("app_agreement_accepted", true).apply()
                v.dismiss()
            } else {
                Toast.makeText(activity, R.string.input_agree_toast, Toast.LENGTH_SHORT).show()
                activity.finish()
            }
        }
        dialog.setNegativeButton(android.R.string.cancel) { _, _ -> activity.finish() }
        dialog.show()
    }
}