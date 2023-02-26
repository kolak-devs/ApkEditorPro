package com.mcal.bshengine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import bsh.Interpreter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.bshengine.adapters.LogAdapter
import com.mcal.bshengine.adapters.PatcherHistoryItem
import com.mcal.bshengine.api.*
import com.mcal.bshengine.databinding.BshengineActivityBinding
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.data.Constants.getDomain
import com.mcal.common.filesystem.FilePickHelper
import com.mcal.common.utils.ActivityHelper.attachParam
import com.mcal.common.utils.ScopedStorage.getPatchesDir
import com.mcal.common.utils.copyFile
import com.mcal.editor.TextEditor.getSoraEditor
import com.mcal.webview.WebViewActivity
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.stream.Collectors
import kotlin.io.path.name

class BshEngineActivity : CustomizedLangActivity() {
    private lateinit var binding: BshengineActivityBinding

    private var mPatchPath: File? = null
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
                        mPatchPath?.takeIf { it.exists() && it.name.endsWith(".bsh") }?.let {
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
                    R.id.menu_patch_history -> {
                        val itemAdapter = ItemAdapter<PatcherHistoryItem>()
                        val fastAdapter = FastAdapter.with(itemAdapter)
                        val fmt = SimpleDateFormat("EEE, HH:mm")
                        val list = arrayListOf<PatcherHistoryItem>()
                        /**
                         * Получаем список файлов в директории патчей. И отображаем на экране все архивы
                         */
                        Files.walk(getPatchesDir().toPath()).filter {
                            it.name.endsWith(".bsh")
                        }.collect(Collectors.toList()).forEach { patchFile ->
                            list.add(
                                PatcherHistoryItem()
                                    .withId(Files.getLastModifiedTime(patchFile).toMillis())
                                    .withIcon(ContextCompat.getDrawable(this@BshEngineActivity, R.drawable.ic_android))
                                    .withTitle(patchFile.name)
                                    .withSubTitle(fmt.format(Files.getLastModifiedTime(patchFile).toMillis()))
                            )
                            itemAdapter.add(list)
                        }

                        if (itemAdapter.adapterItemCount >= 0) {
                            LayoutInflater.from(this@BshEngineActivity).inflate(R.layout.dialog_bsh_patcher_history, null).apply {
                                findViewById<RecyclerView>(R.id.recycler_view).apply {
                                    adapter = fastAdapter
                                    itemAnimator = DefaultItemAnimator()
                                }
                                val dialog = MaterialAlertDialogBuilder(this@BshEngineActivity).create()
                                dialog.setTitle(R.string.select_patch)
                                dialog.setView(this)
                                dialog.show()

                                fastAdapter.onClickListener = { _: View?, _: IAdapter<PatcherHistoryItem>, mainMenuItem: PatcherHistoryItem, _: Int ->
                                    mainMenuItem.title?.let { title ->
                                        mPatchPath = File(getPatchesDir(), title)
                                        binding.filename.setText(title)
                                        dialog.dismiss()
                                    }
                                    true
                                }
                            }
                        }
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
        val log = XLog(logItemAdapter, binding.listLog, fastApkAdapter)
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
                    i["XAlert"] = XAlert(this@BshEngineActivity)

                    withContext(Dispatchers.Main) {
                        binding.listLog.apply {
                            FastScrollerBuilder(this).build();
                            adapter = fastApkAdapter
                            addItemDecoration(DividerItemDecoration(this@BshEngineActivity, DividerItemDecoration.VERTICAL))
                        }
                    }

                    i["XLog"] = log
                    i["XSignature"] = XSignature(decodedDir)

                    if (BuildConfig.DEBUG) {
                        i.eval(InputStreamReader(assets.open("string_encryption.java")))
                    } else {
                        mPatchPath?.takeIf { it.exists() && it.name.endsWith(".bsh") }?.let {
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
                e.message?.let { log.error(it) }
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
                    val patchFile = File(getPatchesDir(), FilePickHelper.getFileName(this, it))
                    contentResolver.openInputStream(it)?.let { inputStream ->
                        copyFile(inputStream, patchFile)
                    }.also {
                        val patchName = patchFile.name
                        if (patchFile.exists() && patchName.endsWith(".bsh")) {
                            binding.filename.setText(patchName)
                            mPatchPath = patchFile
                        } else {
                            Toast.makeText(this, getString(R.string.unsupported_file), Toast.LENGTH_SHORT).show()
                        }
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