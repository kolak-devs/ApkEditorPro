package com.mcal.editor.dialogs

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.common.utils.copyFile
import com.mcal.neweditor.R
import com.mcal.neweditor.databinding.DialogSmaliTemplateBinding
import java.io.*

open class SmaliCodeDialog(private val activity: Activity, filePath: String) {
    private val smaliRootFolder: String
    private val binding = DialogSmaliTemplateBinding.inflate(activity.layoutInflater)
    private val smaliName = arrayOf(
        activity.getString(R.string.show_toast),
        activity.getString(R.string.log_message),
        activity.getString(R.string.dump_value),
        activity.getString(R.string.print_stacktrace)
    )

    init {
        smaliRootFolder = getSmaliRootFolder(filePath)

        val names = arrayOfNulls<String>(smaliName.size)
        System.arraycopy(smaliName, 0, names, 0, smaliName.size)

        binding.spinnerCodename.apply {
            adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, names).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(arg0: AdapterView<*>?, arg1: View, position: Int, arg3: Long) {
                    updateSmaliCode(position)
                }

                override fun onNothingSelected(arg0: AdapterView<*>?) {}
            }
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setPositiveButton(activity.getString(R.string.copy), null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("code", binding.etSamplecode.text.toString())
                clipboard.setPrimaryClip(clip)
                copyUtilSmali()
            }
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setOnClickListener {
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun getSmaliRootFolder(filePath: String): String {
        val dirs = filePath.split("/".toRegex()).toTypedArray()
        val smaliPath = StringBuilder()
        for (dir in dirs) {
            smaliPath.append(dir).append("/")
            if ("smali" == dir || dir.startsWith("smali_")) {
                break
            }
        }
        return smaliPath.toString()
    }

    protected fun updateSmaliCode(position: Int) {
        if (position < smaliCodes.size) {
            binding.etSamplecode.setText(smaliCodes[position])
        }
    }

    // Copy from assets to decode smali folder
    private fun copyUtilSmali() {
        val dirPath = smaliRootFolder + "apkeditor"
        val dir = File(dirPath)
        if (!dir.exists()) {
            dir.mkdir()
        }
        val filePath = "$dirPath/Utils.smali"
        val file = File(filePath)
        if (!file.exists()) {
            var fos: FileOutputStream? = null
            var inputStream: InputStream? = null
            try {
                fos = FileOutputStream(file)
                inputStream = activity.assets.open("smali_patch/Utils.smali")
                copyFile(inputStream, fos)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                closeQuietly(fos)
                closeQuietly(inputStream)
            }
        }
    }

    private fun closeQuietly(c: Closeable?) {
        try {
            c?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private val smaliCodes = arrayOf(
            """    const-string v0, "This is a toast."
    # p0 (this object) must be an object of Context
    invoke-static {p0, v0}, Lapkeditor/Utils;->showToast(Landroid/content/Context;Ljava/lang/String;)V""",
            """    # use 'adb logcat APKEDITOR:* *:S' to view the log
    const-string v0, "I am here."
    invoke-static {v0}, Lapkeditor/Utils;->log(Ljava/lang/String;)V""",
            """    # use 'adb logcat APKEDITOR:* *:S' to view the value
    invoke-static {v0}, Lapkeditor/Utils;->dumpValue(Ljava/lang/Object;)V""",
            """    # use 'adb logcat APKEDITOR:* *:S' to view the stack trace
    invoke-static {}, Lapkeditor/Utils;->printCallStack()V"""
        )
    }
}