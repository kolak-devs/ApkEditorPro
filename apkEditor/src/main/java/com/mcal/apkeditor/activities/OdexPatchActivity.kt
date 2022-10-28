package com.mcal.apkeditor.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.mcal.apkeditor.R
import com.mcal.apkeditor.databinding.ActivityOdexPatchBinding
import com.mcal.apkeditor.utils.OdexPatcher
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.filesystem.FilePickHelper
import com.mcal.common.utils.ApkInfoParser
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.copyFile
import com.mcal.common.view.ProgressDialog
import com.mcal.common.view.ProgressDialog.ProcessingInterface
import java.io.File

/**
 * Created by phe3 on 1/30/2018.
 */
class OdexPatchActivity : CustomizedLangActivity() {
    private lateinit var binding: ActivityOdexPatchBinding
    private lateinit var pickLauncher: ActivityResultLauncher<Intent>
    private var apkPath: String? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOdexPatchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(R.id.toolbar, "Odex Patcher", back = true)
        initView()
    }

    private fun initView() {
        binding.btnSelectApkpath.setOnClickListener {
            pickApk()
        }
        binding.btnApplyPatch.setOnClickListener {
            apkPath?.takeIf { it.isNotEmpty() && File(it).exists() }?.let {
                ProgressDialog(
                    this@OdexPatchActivity, "", "Working…", false,
                    PatchProcessor(), -1
                ).show()
            } ?: run {
                Toast.makeText(this, R.string.msg_unsupported_file, Toast.LENGTH_SHORT).show()
            }
        }
        pickLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    val apk = File(ScopedStorage.getTmpDir().path, FilePickHelper.getFileName(this, uri))
                    contentResolver.openInputStream(uri)?.let { bytes -> copyFile(bytes, apk) }
                    if (apk.exists() && apk.name.endsWith(".apk")) {
                        val path = apk.path.also { apkPath = it }
                        binding.etApkpath.setText(path)
                    } else {
                        Toast.makeText(this, R.string.msg_unsupported_file, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun pickApk() {
        pickLauncher.launch(FilePickHelper.pickFile(true))
    }

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