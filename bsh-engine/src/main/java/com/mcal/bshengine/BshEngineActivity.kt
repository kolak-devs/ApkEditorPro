package com.mcal.bshengine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.MenuProvider
import bsh.Interpreter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.bshengine.api.*
import com.mcal.bshengine.databinding.BshengineActivityBinding
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.activities.WebViewActivity
import com.mcal.common.data.Constants.DOMAIN
import com.mcal.common.utils.ActivityHelper.attachParam
import com.mcal.editor.TextEditor.getSoraEditor
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class BshEngineActivity : CustomizedLangActivity() {
    private lateinit var binding: BshengineActivityBinding
    private var scriptPath: File? = null
    private var mDecodedDir: String? = null
    private var mApkPath: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BshengineActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(R.id.toolbar, "BSH Patcher", back = true)
        binding.btnSelectPatch.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/bsh"
            startActivityForResult(intent, OPEN_REQUEST_CODE)
        }
        binding.btnStartPatch.setOnClickListener {
            try {
                intent.extras?.getString(FILE_PATH)?.takeIf { File(it).exists() }?.let { decodedDir ->
                    mDecodedDir = decodedDir
                    val i = Interpreter()
                    i["XActivity"] = this
                    // API
                    i["XFileHelper"] = XFileHelper()
                    intent.extras?.getString(APK_PATH)?.takeIf { File(it).exists() }?.let { apkPath ->
                        mApkPath = apkPath
                        i["XStorage"] = XStorage(decodedDir, apkPath)
                    } ?: run {
                        Toast.makeText(this, "Apk не найден!", Toast.LENGTH_SHORT).show()
                    }
                    i["XMatcher"] = XMatcher()
                    i["XCipher"] = XCipher()
                    i["XToast"] = XToast(this)
//                    i["XToast.show"] = XToast(this)::class.java.getMethod("show", String::class.java, Boolean::class.java)
                    i["XLog"] = XLog(binding.textLog)
                    i["XSignature"] = XSignature(decodedDir)

                    if (BuildConfig.DEBUG) {
                        i.eval(InputStreamReader(assets.open("bin_patch.java")))
                    } else {
                        scriptPath?.takeIf { it.exists() && it.name.endsWith(".bsh") }?.let {
                            i.eval(InputStreamReader(FileInputStream(it)))
                        } ?: run {
                            Toast.makeText(this, "Неподдерживаемый файл", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: run {
                    Toast.makeText(this, "Директория проекта отсутствует", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val dialog = MaterialAlertDialogBuilder(this)
                dialog.setTitle("Warning")
                dialog.setMessage(e.toString())
                dialog.create()
                dialog.show()
            }
        }
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_bsh_patch, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_patch_edit -> {
                        scriptPath?.takeIf { it.exists() && it.name.endsWith(".bsh") }?.let {
                            val intent = getSoraEditor(this@BshEngineActivity, it.path, null, 0, null)
                            @Suppress("DEPRECATION")
                            startActivityForResult(intent, 0)
                        }
                        return true
                    }
                    R.id.menu_patch_doc -> {
                        val link = "$DOMAIN/apkeditor/doc/bsh-patcher/index.html"
                        val intent = Intent(this@BshEngineActivity, WebViewActivity::class.java)
                        attachParam(intent, "htmlUrl", link)
                        startActivity(intent)
                        return true
                    }
                }
                return false
            }
        })
    }

    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_REQUEST_CODE) {
                resultData?.data?.path?.takeIf { path ->
                    val file = File(path);file.exists() && file.name.endsWith(".bsh")
                }?.let { path ->
                    val file = File(path)
                    binding.textScriptPatch.setText(file.name)
                    scriptPath = file
                } ?: run {
                    Toast.makeText(this, "Неподдерживаемый файл", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(FILE_PATH, mDecodedDir)
        outState.putString(APK_PATH, mApkPath)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        savedInstanceState?.let {
            mDecodedDir = it.getString(FILE_PATH)
            mApkPath = it.getString(APK_PATH)
        }
    }

    companion object {
        private const val FILE_PATH = "filePath"
        private const val APK_PATH = "apkPath"

        private const val OPEN_REQUEST_CODE = 41
    }
}