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
import com.mcal.editor.TextEditor.getSoraEditor
import me.rosuh.filepicker.config.FilePickerManager
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
            FilePickerManager
                .from(this)
                .enableSingleChoice()
                .forResult(FilePickerManager.REQUEST_CODE)
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
                        startActivity(Intent(this@BshEngineActivity, WebViewActivity::class.java))
                        return true
                    }
                }
                return false
            }
        })
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FilePickerManager.REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    File(FilePickerManager.obtainData()[0]).takeIf { it.exists() && it.name.endsWith(".bsh") }?.let {
                        binding.textScriptPatch.setText(it.name)
                        scriptPath = it
                    } ?: run {
                        Toast.makeText(this, "Неподдерживаемый файл", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "You didn't choose anything~", Toast.LENGTH_SHORT).show()
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
        const val FILE_PATH = "filePath"
        const val APK_PATH = "apkPath"
    }
}