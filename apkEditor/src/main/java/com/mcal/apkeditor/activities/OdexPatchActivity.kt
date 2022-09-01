package com.mcal.apkeditor.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.appbar.MaterialToolbar
import com.mcal.apkeditor.R
import com.mcal.apkeditor.dialogs.FileSelectDialog
import com.mcal.apkeditor.dialogs.FileSelectDialog.IFileSelection
import com.mcal.apkeditor.utils.OdexPatcher
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.utils.ApkInfoParser
import com.mcal.common.view.ProgressDialog
import com.mcal.common.view.ProgressDialog.ProcessingInterface
import ru.svolf.melissa.swipeback.SwipeBackActivity

/**
 * Created by phe3 on 1/30/2018.
 */
class OdexPatchActivity : CustomizedLangActivity(), IFileSelection {
    private var apkPathEt: EditText? = null
    private var apkPath: String? = null
    public override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.activity_odex_patch)
        setupToolbar("Odex Patcher")
        initView()
    }

    private fun setupToolbar(title: String) {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun initView() {
        apkPathEt = findViewById<View>(R.id.et_apkpath) as EditText
        val selectBtn = findViewById<View>(R.id.btn_select_apkpath) as Button
        selectBtn.setOnClickListener {
            FileSelectDialog(
                this@OdexPatchActivity,
                this@OdexPatchActivity,
                ".apk",
                "",
                null
            )
        }
        val applyBtn = findViewById<View>(R.id.btn_apply_patch) as Button
        applyBtn.setOnClickListener {
            apkPath = apkPathEt?.text.toString()
            ProgressDialog(
                this@OdexPatchActivity, "", "Working…", false,
                PatchProcessor(), -1
            ).show()
        }
    }

    override fun fileSelectedInDialog(filePath: String?, extraStr: String?, openFile: Boolean) {
        apkPathEt?.setText(filePath)
    }

    override fun isInterestedFile(filename: String?, extraStr: String?): Boolean {
        filename?.let {
            return filename.endsWith(".apk")
        }
        return false
    }

    override fun getConfirmMessage(filePath: String?, extraStr: String?): String? = null

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    internal inner class PatchProcessor : ProcessingInterface {
        private var errMessage: String? = null
        private var odexPath: String? = null

        @Throws(Exception::class)
        override fun process() {
            val parser = ApkInfoParser()
            val info = parser.parse(this@OdexPatchActivity, apkPath) ?: return
            val packageName = info.pkgName
            val patcher = OdexPatcher(packageName)
            patcher.applyPatch(this@OdexPatchActivity, apkPath)
            odexPath = patcher.targetOdex
            patcher.errMessage?.let { msg ->
                errMessage = msg
                throw Exception(errMessage)
            }
        }

        override fun afterProcess() {
            if (errMessage == null) {
                Toast.makeText(this@OdexPatchActivity, "Patched to $odexPath", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }
}