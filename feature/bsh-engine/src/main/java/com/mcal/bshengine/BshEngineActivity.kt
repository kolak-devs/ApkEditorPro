package com.mcal.bshengine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.DividerItemDecoration
import bsh.Interpreter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.bshengine.adapters.LogAdapter
import com.mcal.bshengine.api.*
import com.mcal.bshengine.databinding.BshengineActivityBinding
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.data.Constants.getDomain
import com.mcal.common.filesystem.FilePickHelper
import com.mcal.common.utils.ActivityHelper.attachParam
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.copyFile
import com.mcal.editor.TextEditor.getSoraEditor
import com.mcal.webview.WebViewActivity
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class BshEngineActivity : CustomizedLangActivity() {
    private lateinit var binding: BshengineActivityBinding

    private var scriptPath: File? = null
    private var mDecodedDir: String? = null
    private var mApkPath: String? = null

    private val logItemAdapter by lazy {
        ItemAdapter<LogAdapter>()
    }

    private val fastApkAdapter = FastAdapter.with(logItemAdapter)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BshengineActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(R.id.toolbar, "BSH Patcher", back = true)
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
                        val link = "${getDomain()}/apkeditor/doc/bsh-patcher/index.html"
                        val intent = Intent(this@BshEngineActivity, WebViewActivity::class.java)
                        attachParam(intent, WebViewActivity.HTML_URL, link)
                        startActivity(intent)
                        return true
                    }
                }
                return false
            }
        })
        binding.funcSelect.setOnClickListener {
            @Suppress("DEPRECATION")
            startActivityForResult(FilePickHelper.pickFile(false), OPEN_REQUEST_CODE);
        }
        binding.funcApply.setOnClickListener {
            it.isEnabled = false
            binding.funcSelect.isEnabled = false
            binding.tickTimer.base = SystemClock.elapsedRealtime()
            binding.tickTimer.start()
            CoroutineScope(Dispatchers.IO).launch {
                startPatching()
                withContext(Dispatchers.Main) {
                    it.isEnabled = true
                    binding.funcSelect.isEnabled = true
                    binding.tickTimer.stop()
                }
            }
        }
    }

    private suspend fun startPatching(): Boolean = withContext(Dispatchers.IO) {
        var result = true
        try {
            intent.extras?.let { bundle ->
                bundle.getString(FILE_PATH)?.takeIf { File(it).exists() }?.let { decodedDir ->
                    mDecodedDir = decodedDir
                    val i = Interpreter()
                    i["XActivity"] = this@BshEngineActivity
                    // API
                    i["XFileHelper"] = XFileHelper()
                    i["XStorage"] = XStorage().apply {
                        setDecodedDir(decodedDir)
                        setApkPath(bundle.getString(APK_PATH))
                    }
                    i["XMatcher"] = XMatcher()
                    i["XString"] = XString()
                    i["XCipher"] = XCipher()
                    i["XToast"] = XToast(this@BshEngineActivity)

                    withContext(Dispatchers.Main) {
                        binding.listLog.apply {
                            FastScrollerBuilder(this).build();
                            adapter = fastApkAdapter
                            addItemDecoration(DividerItemDecoration(this@BshEngineActivity, DividerItemDecoration.VERTICAL))
                        }
                    }

                    i["XLog"] = XLog(logItemAdapter, binding.listLog, fastApkAdapter)
                    i["XSignature"] = XSignature(decodedDir)

                    if (BuildConfig.DEBUG) {
                        i.eval(InputStreamReader(assets.open("string_encryption.java")))
                    } else {
                        scriptPath?.takeIf { it.exists() && it.name.endsWith(".bsh") }?.let {
                            i.eval(InputStreamReader(FileInputStream(it)))
                        } ?: run {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@BshEngineActivity, getString(R.string.unsupported_file), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@BshEngineActivity, getString(R.string.not_found_project_dir), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            result = false
            withContext(Dispatchers.Main) {
                val dialog = MaterialAlertDialogBuilder(this@BshEngineActivity)
                dialog.setMessage(e.toString())
                dialog.create()
                dialog.show()
            }
        }
        result
    }

    @Suppress("OVERRIDE_DEPRECATION")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_REQUEST_CODE) {
                resultData?.data?.let {
                    val script = File(ScopedStorage.getTmpDir().path, FilePickHelper.getFileName(this, it))
                    contentResolver.openInputStream(it)?.let { it1 -> copyFile(it1, script) }
                    if (script.exists() && script.name.endsWith(".bsh")) {
                        val file = File(script.path)
                        binding.filename.setText(file.name)
                        scriptPath = file
                    } else {
                        Toast.makeText(this, getString(R.string.unsupported_file), Toast.LENGTH_SHORT).show()
                    }
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
        savedInstanceState?.let { bundle ->
            mDecodedDir = bundle.getString(FILE_PATH)
            mApkPath = bundle.getString(APK_PATH)
        }
    }

    companion object {
        const val FILE_PATH = "filePath"
        const val APK_PATH = "apkPath"

        private const val OPEN_REQUEST_CODE = 41
    }
}