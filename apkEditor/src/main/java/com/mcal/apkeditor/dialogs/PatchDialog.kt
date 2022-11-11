package com.mcal.apkeditor.dialogs

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.R
import com.mcal.apkeditor.dialogs.FileSelectDialog.IFileSelection
import com.mcal.apkeditor.patch.PatchExecutor
import com.mcal.apkeditor.patch.interfaces.ApkInfoListener
import com.mcal.apkeditor.patch.interfaces.IPatchContext
import com.mcal.common.utils.LocaleManager
import com.mcal.common.utils.copyFile
import com.mcal.common.utils.makeDir
import com.mcal.common.utils.readText
import com.mcal.patchview.ui.CodeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.xml.sax.SAXException
import ru.mcal.manifestparser.xml.AndroidManifestParser
import java.io.*
import java.util.zip.ZipFile
import javax.xml.parsers.ParserConfigurationException

// Dialog used for patch applying
class PatchDialog(activity: Activity, private val listener: ApkInfoListener) : View.OnClickListener,
    IPatchContext {
    private val mActivity = activity

    // Record all the global parameter values
    private val globalVariableValues: MutableMap<String, String> = HashMap()
    private var exampleDir: String? = null
    private var patchPath: String? = null
    private var patchPathTv: TextView? = null
    private var webView: WebView? = null
    private var logLayout: View? = null
    private var logTv: CodeText? = null

    // Record executor as the parse is done there
    private var patchExecutor: PatchExecutor? = null
    private var materialDialog: AlertDialog? = null
    private val manifestPath: File = File(listener.decodeRootPath + "/AndroidManifest.xml")

    private fun init(activity: Activity) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_patch, null)
        val curPatchTv = view.findViewById<TextView>(R.id.tv_curpatch)
        curPatchTv.setOnClickListener(this)
        patchPathTv = view.findViewById(R.id.tv_patch_path)
        val saveExamplesTv = view.findViewById<TextView>(R.id.tv_save_patches)
        saveExamplesTv.setOnClickListener(this)
        webView = view.findViewById(R.id.web_instructions)
        CoroutineScope(Dispatchers.Main).launch {
            webView?.loadUrl("file:///android_asset/doc/" + LocaleManager.getDocLanguage() + "/patch.html")
        }
        logLayout = view.findViewById(R.id.log_layout)
        logTv = view.findViewById(R.id.tv_patchlog)
        materialDialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .setPositiveButton(activity.getString(R.string.select_patch), null)
            .setNegativeButton(activity.getString(R.string.close), null)
            .create()
        materialDialog?.setOnShowListener {
            materialDialog?.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                if (patchPath == null) {
                    selectPatch()
                } else {
                    applyPatch()
                }
            }
        }
        materialDialog?.show()
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.btn_close) {
            materialDialog?.dismiss()
        } else if (id == R.id.tv_curpatch) {
            selectPatch()
        } else if (id == R.id.tv_save_patches) {
            // ApkEditor Patches
            var ret = extractExamples("patch_AE_app_rename.zip")
            ret = ret or extractExamples("patch_AE_bypass_sign_check.zip")
            ret = ret or extractExamples("patch_AE_data_editor.zip")
            ret = ret or extractExamples("patch_AE_launcher_toast.zip")
            ret = ret or extractExamples("patch_AE_mem_editor.zip")
            ret = ret or extractExamples("patch_AE_my_font.zip")
            ret = ret or extractExamples("patch_AE_new_entrance.zip")
            ret = ret or extractExamples("patch_AE_script_example.zip")

            // Kubarev Patches
            ret = ret or extractExamples("patch_Bin_SignHook.zip")
            ret = ret or extractExamples("patch_CNFIX_3.0_SignHook.zip")
            ret = ret or extractExamples("patch_Heavenly_SignHook.zip")
            ret = ret or extractExamples("patch_LP_DexSignHook.zip")
            ret = ret or extractExamples("patch_LP_SignHook.zip")
            ret = ret or extractExamples("patch_R3Tools_SignHook.zip")
            ret = ret or extractExamples("patch_Ultima_SignHook.zip")
            ret = ret or extractExamples("patch_Ultima_VipSignHook.zip")
            if (ret) {
                val message =
                    String.format(mActivity.getString(R.string.patch_examples_copied), exampleDir)
                Toast.makeText(mActivity, message, Toast.LENGTH_SHORT)
                    .show()
            }

//            mActivity.assets.list("a")?.forEach { patch ->
//                extractExamples(patch)
//            }

        }
    }

    private fun initExampleDir() {
        // Check the directory exist or not
        if (exampleDir == null) {
            try {
                exampleDir = makeDir("patches").path
            } catch (e1: Exception) {
                e1.printStackTrace()
            }
        }
    }

    private fun extractExamples(filename: String): Boolean {
        initExampleDir()
        val path = exampleDir + File.separator + filename
        val am = mActivity.assets
        var input: InputStream? = null
        var output: FileOutputStream? = null
        try {
            input = am.open("patches" + File.separator + filename)
            output = FileOutputStream(path)
            copyFile(input, output)
            return true
        } catch (e: IOException) {
            Toast.makeText(
                this.mActivity, e.message,
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            closeQuietly(input)
            closeQuietly(output)
        }
        return false
    }

    private fun closeQuietly(close: Closeable?) {
        close?.let {
            try {
                close.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun closeQuietly(zfile: ZipFile?) {
        zfile?.let {
            try {
                zfile.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun applyPatch() {
        // Switch the view
        logTv?.text = ""

        materialDialog?.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = false
        materialDialog?.show()

        // Patch it
        // TODO: Multiple select patches
        patchExecutor = PatchExecutor(mActivity, listener, patchPath, this)
        patchExecutor?.applyPatch()
    }

    private fun selectPatch() {
        var defaultDir: String? = null
        initExampleDir()
        exampleDir?.let { dir ->
            if (File(dir).exists()) {
                defaultDir = exampleDir
            }
        }
        FileSelectDialog(
            mActivity,
            object : IFileSelection {
                override fun fileSelectedInDialog(
                    filePath: String?, extraStr: String?, openFile: Boolean
                ) {
                    filePath?.let {
                        patchSelected(filePath)
                    }
                }

                override fun isInterestedFile(filename: String?, extraStr: String?): Boolean {
                    filename?.let {
                        return filename.endsWith(".zip")
                    }
                    return false
                }

                override fun getConfirmMessage(filePath: String?, extraStr: String?): String? {
                    return null
                }

            },
            ".zip",
            null,
            mActivity.getString(R.string.select_patch),
            false,
            false,
            false,
            "patch",
            defaultDir
        )
    }

    private fun patchSelected(filePath: String) {
        logLayout?.visibility = View.VISIBLE
        webView?.visibility = View.GONE
        val patchConfig = getPatchConfig(filePath)
        patchConfig?.let { config ->
            logTv?.text = config
        }
        patchPath = filePath
        patchPathTv?.text = patchPath
        materialDialog?.getButton(DialogInterface.BUTTON_POSITIVE)
            ?.setText(R.string.apply_the_patch)
        materialDialog?.show()
    }

    private fun getPatchConfig(filePath: String): String? {
        var zfile: ZipFile? = null
        var input: InputStream? = null
        try {
            zfile = ZipFile(filePath)
            val entry = zfile.getEntry("patch.txt")
            if (entry == null) {
                this.error(R.string.patch_error_no_entry, "patch.txt")
            }
            input = zfile.getInputStream(entry)
            return input.readText()
        } catch (e: Exception) {
            e.message?.let { message ->
                error(R.string.general_error, message)
            }
        } finally {
            closeQuietly(input)
            closeQuietly(zfile)
        }
        return null
    }

    @Suppress("SAFE_CALL_WILL_CHANGE_NULLABILITY")
    override fun info(resourceId: Int, bold: Boolean, vararg args: Any) {
        var txt = mActivity.getString(resourceId)
        @Suppress("UNNECESSARY_SAFE_CALL")
        args?.let {
            txt = String.format(txt, *args)
        }
        val message = if (bold) "\n" + txt + "\n" else txt + "\n"
        appendText(message, bold, false)
    }

    @Suppress("SAFE_CALL_WILL_CHANGE_NULLABILITY")
    override fun info(format: String, bold: Boolean, vararg args: Any) {
        var txt = format
        @Suppress("UNNECESSARY_SAFE_CALL")
        args?.let {
            txt = String.format(txt, *args)
        }
        val message = if (bold) "\n" + txt + "\n" else txt + "\n"
        appendText(message, bold, false)
    }

    @Suppress("SAFE_CALL_WILL_CHANGE_NULLABILITY")
    override fun error(resourceId: Int, vararg args: Any) {
        var txt = mActivity.getString(resourceId)
        @Suppress("UNNECESSARY_SAFE_CALL")
        args?.let {
            txt = String.format(txt, *args)
        }
        appendText(txt + "\n", bold = false, red = true)
    }

    private fun appendText(
        txt: String, bold: Boolean,
        red: Boolean
    ) {
        mActivity.runOnUiThread {
            if (red) {
                val spanString = SpannableString(txt)
                val span = ForegroundColorSpan(Color.RED)
                spanString.setSpan(span, 0, txt.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                logTv?.append(spanString)
            } else if (bold) {
                val spanString = SpannableString(txt)
                val span = StyleSpan(Typeface.BOLD)
                spanString.setSpan(
                    span, 0, txt.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                logTv?.append(spanString)
            } else {
                logTv?.append(txt)
            }
        }
    }

    override fun getString(stringId: Int): String {
        return mActivity.getString(stringId)
    }

    override fun setVariableValue(key: String, value: String) {
        globalVariableValues[key] = value
    }

    override fun getVariableValue(key: String): String? {
        globalVariableValues[key]?.let {
            return it
        }
        return null
    }

    override fun patchFinished() {
        mActivity.runOnUiThread {
            materialDialog?.getButton(DialogInterface.BUTTON_POSITIVE)
                ?.setText(R.string.patch_applied)
            materialDialog?.getButton(DialogInterface.BUTTON_POSITIVE)
                ?.setBackgroundColor(-0x9f9fa0)
            materialDialog?.show()
        }
    }

    override fun getDecodeRootPath(): String {
        return listener.decodeRootPath
    }

    override fun getSmaliFolders(): List<String> {
        val folders: MutableList<String> = ArrayList()
        folders.add("smali")

        // Look into zip file to get all dex, thus get related smali folder
        val apkPath = listener.apkPath
        var zfile: ZipFile? = null
        try {
            zfile = ZipFile(apkPath)
            val entries = zfile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                if (name.endsWith(".dex") && !name.contains("/")) {
                    if (name != "classes.dex") {
                        folders.add(
                            "smali_" + name.substring(0, name.length - 4)
                        )
                    }
                }
            }
        } catch (e1: IOException) {
            e1.printStackTrace()
        } finally {
            closeQuietly(zfile)
        }
        return folders
    }

    override fun getApplicationName(): String? {
        try {
            val parser = AndroidManifestParser.parse(FileInputStream(manifestPath))
            val name = parser.applicationName
            name?.let {
                return name
            }
        } catch (e: ParserConfigurationException) {
            e.printStackTrace()
        } catch (e: SAXException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    @Suppress("SAFE_CALL_WILL_CHANGE_NULLABILITY")
    override fun getActivities(): List<String>? {
        val activityName: MutableList<String> = ArrayList()
        try {
            val parser = AndroidManifestParser.parse(FileInputStream(manifestPath))
            for (activity in parser.activities) {
                activityName.add(activity.name)
            }
            @Suppress("UNNECESSARY_SAFE_CALL")
            activityName?.let {
                return activityName
            }
        } catch (e: ParserConfigurationException) {
            e.printStackTrace()
        } catch (e: SAXException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    @Suppress("SAFE_CALL_WILL_CHANGE_NULLABILITY")
    override fun getLauncherActivities(): List<String>? {
        val activityName: MutableList<String> = ArrayList()
        try {
            val parser = AndroidManifestParser.parse(FileInputStream(manifestPath))
            activityName.add(parser.launcherActivity.name)
            @Suppress("UNNECESSARY_SAFE_CALL")
            activityName?.let {
                return activityName
            }
        } catch (e: ParserConfigurationException) {
            e.printStackTrace()
        } catch (e: SAXException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun getPatchNames(): List<String>? {
        patchExecutor?.let { executor ->
            return executor.ruleNames
        }
        return null
    }

    init {
        init(activity)
    }
}