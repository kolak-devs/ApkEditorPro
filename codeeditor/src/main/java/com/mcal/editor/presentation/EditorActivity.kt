package com.mcal.editor.presentation

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Typeface
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.UiAction
import com.mcal.colorconverter.ColorPickerConverter
import com.mcal.colormixer.ColorMixer
import com.mcal.colormixer.ColorMixerDialog
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.data.ReactivePreferences.isIgnoreCaseAsync
import com.mcal.common.data.ReactivePreferences.isUseRegexAsync
import com.mcal.common.data.ReactivePreferences.setIgnoreCase
import com.mcal.common.data.ReactivePreferences.setUseRegex
import com.mcal.common.utils.ClipboardUtils.copyToClipboard
import com.mcal.common.utils.copyBack
import com.mcal.common.view.ProgressDialog
import com.mcal.editor.dialogs.DecToHexConverter
import com.mcal.editor.dialogs.DexToJava
import com.mcal.editor.dialogs.SmaliCodeDialog
import com.mcal.editor.dialogs.SmaliToJava
import com.mcal.editor.navigation.CodeNavigationDialog
import com.mcal.editor.utils.FileUtils
import com.mcal.neweditor.R
import com.mcal.neweditor.databinding.ActivitySoraeditorBinding
import com.mcal.presentation.base.BaseActivity
import io.github.rosemoe.sora.event.*
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.text.LineSeparator
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.component.Magnifier
import io.github.rosemoe.sora.widget.style.builtin.ScaleCursorAnimator
import io.github.rosemoe.sora.widget.subscribeEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.tm4e.core.registry.IGrammarSource
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.File
import java.io.IOException
import java.util.regex.PatternSyntaxException


class EditorActivity : BaseActivity<EditorViewModel, ActivitySoraeditorBinding>(
    ActivitySoraeditorBinding::inflate
), CodeNavigationDialog.ISmaliMethodClicked, ColorMixer.OnColorChangedListener {

    override fun viewModelClass() = EditorViewModel::class.java

    private var save: MenuItem? = null
    private var undo: MenuItem? = null
    private var redo: MenuItem? = null
    private var dexToJava: MenuItem? = null
    private var smaliToJava: MenuItem? = null
    private var methodsList: MenuItem? = null
    private var templatesMenu: MenuItem? = null

    private var mFilePath: File? = null
    private var mApkPath: File? = null
    private var realFilePath: String? = null // when not null, need to copy back to real path
    private var isRootMode = false
    private var resIds: List<Int>? = null
    private var startLineList: List<Int>? = null
    private var filePathList: ArrayList<String>? = null
    private var curFileIndex = 0

    //private var resIdTooBig = -1
    private var resIdFileSaved = -1
    private var resIdNotFound = -1
    private var startLine = 0

    override fun callOperations() = with(viewModel) {
        setupToolbar(id = R.id.toolbar, title = getString(R.string.title_code_editor), back = false)
        initIntent()
        getFileName()
        initSymbolsList()
        initSearchEditor()
        initEditor()
        openFile()
        viewModel.updatePositionText(binding.editor.cursor, binding.editor.text)
        updateBtnState()
        viewModel.setupDiagnostics(binding.editor.text)
    }

    override fun onSetupLayout() = with(binding) {
        buttonGotoNext.setOnClickListener {
            try {
                binding.editor.searcher.gotoNext()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
        buttonGotoLast.setOnClickListener {
            try {
                binding.editor.searcher.gotoPrevious()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
        buttonReplaceAll.setOnClickListener {
            try {
                binding.editor.searcher.replaceAll(binding.replaceEditor.text.toString())
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
        buttonReplace.setOnClickListener {
            try {
                binding.editor.searcher.replaceThis(binding.replaceEditor.text.toString())
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
        menu.setOnClickListener { view ->
            val popupMenu = PopupMenu(this@EditorActivity, view)
            popupMenu.menuInflater.inflate(R.menu.menu_editor_search, popupMenu.menu)
            popupMenu.menu.findItem(R.id.use_regex).isChecked = isUseRegexAsync()
            popupMenu.menu.findItem(R.id.ignore_case).isChecked = isIgnoreCaseAsync()
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.use_regex ->
                        CoroutineScope(Dispatchers.Main).launch {
                            setUseRegex(!item.isChecked)
                            updateSearchState()
                        }
                    R.id.ignore_case ->
                        CoroutineScope(Dispatchers.Main).launch {
                            setIgnoreCase(!item.isChecked)
                            updateSearchState()
                        }
                    R.id.close ->
                        popupMenu.dismiss()
                }
                true
            }
            popupMenu.show()
        }
    }

    /**
     * Обновляет результаты поиска
     */
    private fun updateSearchState() {
        try {
            binding.searchEditor.text.toString().takeIf { it.isNotEmpty() }?.let {
                binding.editor.searcher.search(
                    it,
                    EditorSearcher.SearchOptions(isIgnoreCaseAsync(), isUseRegexAsync())
                )
            }
        } catch (e: PatternSyntaxException) {
            e.printStackTrace()
            // Regex error
        }
    }

    override fun onBindViewModel() = with(viewModel) {
        updatePositionText.observe(this@EditorActivity) { text ->
            binding.positionDisplay.text = text
        }
        setupDiagnostics.observe(this@EditorActivity) { container ->
            binding.editor.diagnostics = container
        }
    }

    private fun initEditor() {
        val editor = binding.editor
        editor.apply {
            lineSeparator = LineSeparator.CRLF
            typefaceText = Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf")
            setLineSpacing(2f, 1.1f)
            // Update display dynamically
            subscribeEvent<SelectionChangeEvent> { _, _ ->
                viewModel.updatePositionText(binding.editor.cursor, binding.editor.text)
            }
            subscribeEvent<ContentChangeEvent> { _, _ ->
                postDelayed(::updateBtnState, 50)
            }
//            subscribeEvent<SideIconClickEvent> { _, _ ->
//                Toast.makeText(this@EditorActivity, "Side icon clicked", Toast.LENGTH_SHORT).show()
//            }

//            subscribeEvent<KeyBindingEvent> { event, _ ->
//                if (event.eventType != EditorKeyEvent.Type.DOWN) {
//                    return@subscribeEvent
//                }
//
//                Toast.makeText(
//                    context,
//                    "Keybinding event: " + generateKeybindingString(event),
//                    Toast.LENGTH_LONG
//                ).show()
//            }
            // Custom cursor animator
            cursorAnimator = ScaleCursorAnimator(editor)

            lifecycleScope.launch {
                if (ReactivePreferences.isShowUnprintable()) {
                    nonPrintablePaintingFlags =
                        CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or CodeEditor.FLAG_DRAW_LINE_SEPARATOR or CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION
                }
                colorScheme = getCodeColorScheme()
                editor.isWordwrap = ReactivePreferences.isWordWrap()
                editor.isLineNumberEnabled = ReactivePreferences.isLineNumberEnabled()
                editor.setPinLineNumber(ReactivePreferences.isLineNumberPinned())
                editor.getComponent(Magnifier::class.java).isEnabled = ReactivePreferences.isMagnifier()
                editor.props.useICULibToSelectWords = ReactivePreferences.isUseICULibrary()
                setEditorLanguage(getLanguage())
                setTextSize(ReactivePreferences.getFontSize().toFloat())
            }
        }
    }

//    private fun generateKeybindingString(event: KeyBindingEvent): String {
//        val sb = StringBuilder()
//        if (event.isCtrlPressed) {
//            sb.append("Ctrl + ")
//        }
//
//        if (event.isAltPressed) {
//            sb.append("Alt + ")
//        }
//
//        if (event.isShiftPressed) {
//            sb.append("Shift + ")
//        }
//
//        sb.append(KeyEvent.keyCodeToString(event.keyCode))
//        return sb.toString()
//    }

    private fun initSearchEditor() {
        binding.searchEditor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (editable.isNotEmpty()) {
                    try {
                        binding.editor.searcher.search(
                            editable.toString(),
                            EditorSearcher.SearchOptions(isIgnoreCaseAsync(), isUseRegexAsync())
                        )
                    } catch (e: PatternSyntaxException) {
                        e.printStackTrace()
                        // Regex error
                    }
                } else {
                    binding.editor.searcher.stopSearch()
                }
            }
        })
    }

    private fun initSymbolsList() {
        val inputView = binding.symbolInput
        inputView.bindEditor(binding.editor)
        inputView.setTextColor(R.color.colorGray)
        inputView.addSymbols(
            arrayOf("->", "{", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/"),
            arrayOf("\t", "{}", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/")
        )
    }

    private suspend fun getCodeColorScheme(): TextMateColorScheme {
        return TextMateColorScheme(
            if (ReactivePreferences.isNightMode()) {
                getDarkTheme()
            } else {
                getLightTheme()
            }
        )
    }

    private fun getDarkTheme(): IThemeSource? {
        return try {
            IThemeSource.fromInputStream(
                assets.open("textmate/dark.json"),
                "dark.json",
                null
            )
        } catch (e: java.lang.Exception) {
            throw RuntimeException(e)
        }
    }

    private fun getLightTheme(): IThemeSource? {
        return try {
            IThemeSource.fromInputStream(
                assets.open("textmate/light.tmTheme"),
                "light.tmTheme",
                null
            )
        } catch (e: java.lang.Exception) {
            throw RuntimeException(e)
        }
    }

    private fun getLanguage(): TextMateLanguage? {
        mFilePath?.name?.let { fileName ->
            return if (fileName.endsWith(".smali")) {
                getTextMateLanguage("smali.tmLanguage.json", "textmate/smali/syntaxes/smali.tmLanguage.json")
            } else if (fileName.endsWith(".java") || fileName.endsWith(".bsh")) {
                getTextMateLanguage("java.tmLanguage.json", "textmate/java/syntaxes/java.tmLanguage.json")
            } else if (fileName.endsWith(".kt")) {
                getTextMateLanguage("kotlin.tmLanguage.json", "textmate/kotlin/syntaxes/kotlin.tmLanguage")
            } else if (fileName.endsWith(".groovy") || fileName.endsWith(".gradle")) {
                getTextMateLanguage("groovy.tmLanguage.json", "textmate/groovy/syntaxes/groovy.tmLanguage")
            } else if (fileName.endsWith(".json")) {
                getTextMateLanguage("json.tmLanguage.json", "textmate/json/syntaxes/json.tmLanguage.json")
            } else if (fileName.endsWith(".xml")) {
                getTextMateLanguage("xml.tmLanguage.json", "textmate/xml/syntaxes/xml.tmLanguage.json")
            } else if (fileName.endsWith(".html")) {
                getTextMateLanguage("html.tmLanguage.json", "textmate/html/syntaxes/html.tmLanguage.json")
            } else if (fileName.endsWith(".js")) {
                getTextMateLanguage("javascript.tmLanguage.json", "textmate/javascript/syntaxes/JavaScript.tmLanguage.json")
            } else if (fileName.endsWith(".mk")) {
                getTextMateLanguage("markdown.tmLanguage.json", "textmate/markdown/syntaxes/markdown.tmLanguage.json")
            } else if (fileName.endsWith(".py")) {
                getTextMateLanguage("python.tmLanguage.json", "textmate/python/syntaxes/python.tmLanguage.json")
            } else {
                getTextMateLanguage("java.tmLanguage.json", "textmate/java/syntaxes/java.tmLanguage.json")
            }
        }
        return null
    }

    private fun getTextMateLanguage(name: String, path: String): TextMateLanguage? {
        return try {
            TextMateLanguage.create(
                IGrammarSource.fromInputStream(
                    assets.open(path),
                    name,
                    null
                ),
                null,
                getDarkTheme()
            )
        } catch (e: java.lang.Exception) {
            throw RuntimeException(e)
        }
    }

    private var extraString: String? = null

    private fun initIntent() {
        extraString = intent.getStringExtra("extraString")
        filePathList = intent.getStringArrayListExtra("fileList")
        mFilePath = File(intent.getStringExtra("filePath").toString())
        curFileIndex = intent.getIntExtra("curFileIndex", 0)
        startLine = intent.getIntExtra("startLine", 0)
        startLineList = intent.getIntegerArrayListExtra("startLineList")
        mApkPath = File(intent.getStringExtra("apkPath").toString())
        realFilePath = intent.getStringExtra("realFilePath")
        isRootMode = intent.getBooleanExtra("isRootMode", false)
        resIds = intent.getIntegerArrayListExtra("resourceIds")
        resIds?.let { id ->
            //resIdTooBig = id[0]
            resIdFileSaved = id[1]
            resIdNotFound = id[2]
        }
    }

    private fun copyBack2RealPath(realPath: String) {
        try {
            mFilePath?.let { path ->
                copyBack(path.path, realPath, isRootMode)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun openFile() {
        mFilePath?.let { path ->
            Thread {
                try {
                    val text = FileUtils.readFileAsTextUsingInputStream(path.path)
                    runOnUiThread {
                        binding.toolbarFilename.text = path.name
                        supportActionBar?.apply { title = path.name }
                        binding.editor.apply {
                            setText(text, null)
                            startLineList?.let { line ->
                                startLine = line[curFileIndex]// - 1
                            }
                            if (startLine >= 0 && this.lineCount >= startLine) {
                                jumpToLine(startLine)
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }.start()
            viewModel.updatePositionText(binding.editor.cursor, binding.editor.text)
            updateBtnState()
        }
    }

    private fun getFileName() {
        filePathList?.let { list ->
            mFilePath = File(list[curFileIndex])
        }
        realFilePath?.let { path ->
            mFilePath = File(path)
        }
    }

    @SuppressLint("FileEndsWithExt")
    private fun updateBtnState() {
        if (undo == null) {
            return
        }
        save?.isEnabled = canSave()
        undo?.isEnabled = binding.editor.canUndo()
        redo?.isEnabled = binding.editor.canRedo()
        mFilePath?.let { path ->
            dexToJava?.isVisible = path.name.endsWith(".smali")
            smaliToJava?.isVisible = path.name.endsWith(".smali")
            methodsList?.isVisible = path.name.endsWith(".smali") or path.name.endsWith(".java")
            templatesMenu?.isVisible = path.name.endsWith(".smali")
        }
    }

    private fun canSave(): Boolean {
        return binding.editor.canUndo() || binding.editor.canRedo()
    }

    private val loadTMLLauncher = registerForActivityResult(GetContent()) { result: Uri? ->
        try {
            if (result == null) return@registerForActivityResult
            //TextMateLanguage only support TextMateColorScheme
            var editorColorScheme = binding.editor.colorScheme
            if (editorColorScheme !is TextMateColorScheme) {
                val themeSource = IThemeSource.fromInputStream(
                    assets.open("textmate/QuietLight.tmTheme"),
                    "QuietLight.tmTheme",
                    null
                )
                editorColorScheme = TextMateColorScheme.create(themeSource)
                binding.editor.colorScheme = editorColorScheme
            }
            val language = TextMateLanguage.create(
                IGrammarSource.fromInputStream(
                    contentResolver.openInputStream(result),
                    result.path, null
                ),
                null,
                (editorColorScheme as TextMateColorScheme).themeSource
            )
            binding.editor.setEditorLanguage(language)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val loadTMTLauncher = registerForActivityResult(GetContent()) { result: Uri? ->
        try {
            if (result == null) return@registerForActivityResult
            val iRawTheme = IThemeSource.fromInputStream(
                contentResolver.openInputStream(result), result.path,
                null
            )
            val colorScheme = TextMateColorScheme.create(iRawTheme)
            binding.editor.colorScheme = colorScheme
            val language = binding.editor.editorLanguage
            if (language is TextMateLanguage) {
                language.updateTheme(iRawTheme)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        save = menu.findItem(R.id.text_save)
        undo = menu.findItem(R.id.text_undo)
        redo = menu.findItem(R.id.text_redo)
        dexToJava = menu.findItem(R.id.dex_to_java)
        smaliToJava = menu.findItem(R.id.smali_to_java)
        methodsList = menu.findItem(R.id.methods)
        templatesMenu = menu.findItem(R.id.template)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.editor.release()
    }

    // Save the document
    private fun save(exit: Boolean = false) {
        ProgressDialog(
            this, "Saving", "Please wait...", false,
            object : ProgressDialog.ProcessingInterface {
                @Throws(java.lang.Exception::class)
                override fun process() {
                    CoroutineScope(Dispatchers.IO).launch {
                        mFilePath?.let {
                            FileUtils.writeText(it.path, binding.editor.text.toString())
                        }

                        realFilePath?.let { path ->
                            copyBack2RealPath(path)
                        }
                        withContext(Dispatchers.Main) {
                            openFile()
                            viewModel.updatePositionText(binding.editor.cursor, binding.editor.text)
                            updateBtnState()
                            if (exit) {
                                finish()
                            }
                        }
                    }
                }

                override fun afterProcess() = Unit
            }, resIdFileSaved
        ).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (canSave()) {
            val dialog = MaterialAlertDialogBuilder(this)
            mFilePath?.name?.let { name ->
                dialog.setTitle(name)
            }
            dialog.setMessage(getString(R.string.message_save_file))
            dialog.setPositiveButton(getString(R.string.save)) { _, _ ->
                save(true)
            }
            dialog.setNegativeButton(android.R.string.cancel) { _, _ ->
                super.onBackPressed()
            }
            dialog.show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val editor = binding.editor
        when (id) {
            R.id.color_converter -> {
                ColorPickerConverter(this, 0xffff0000.toInt()).show()
            }
            R.id.dec2hex -> {
                DecToHexConverter(this).show()
            }
            R.id.pallete -> {
                ColorMixerDialog(this, 0xFFFFFF, this)
            }
            R.id.methods -> {
                showNavigationMethods()
            }
            R.id.template -> {
                mFilePath?.let {
                    SmaliCodeDialog(this, it.path)
                }
            }
            R.id.smali_to_java -> {
                mFilePath?.let { filePath ->
                    SmaliToJava(this, filePath).show()
                }
            }
            R.id.dex_to_java -> {
                mFilePath?.let { filePath ->
                    mApkPath?.let { apkPath ->
                        DexToJava(this, filePath, apkPath).show()
                    }
                }
            }
            R.id.text_save -> {
                save()
            }
            R.id.text_undo -> {
                editor.undo()
            }
            R.id.text_redo -> {
                editor.redo()
            }
            R.id.goto_end -> {
                editor.setSelection(
                    editor.text.lineCount - 1,
                    editor.text.getColumnCount(editor.text.lineCount - 1)
                )
            }
            R.id.move_up -> {
                editor.moveSelectionUp()
            }
            R.id.move_down -> {
                editor.moveSelectionDown()
            }
            R.id.home -> {
                editor.moveSelectionHome()
            }
            R.id.end -> {
                editor.moveSelectionEnd()
            }
            R.id.move_left -> {
                editor.moveSelectionLeft()
            }
            R.id.move_right -> {
                editor.moveSelectionRight()
            }
            R.id.code_format -> {
                val cursor = editor.text.cursor
                if (cursor.isSelected) {
                    editor.formatCodeAsync(cursor.left(), cursor.right())
                } else {
                    editor.formatCodeAsync()
                }
            }
            R.id.switch_language -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.switch_language)
                    .setSingleChoiceItems(
                        arrayOf(
                            "Groovy",
                            "Java",
                            "Json",
                            "Kotlin",
                            "Smali",
                            "Xml",
                            "Html",
                            "JavaScript",
                            "MarkDown",
                            "Python",
                            "None"
                        ), -1
                    ) { dialog: DialogInterface, which: Int ->
                        when (which) {
                            0 -> editor.setEditorLanguage(getTextMateLanguage("groovy.tmLanguage.json", "textmate/groovy/syntaxes/groovy.tmLanguage"))
                            1 -> editor.setEditorLanguage(getTextMateLanguage("java.tmLanguage.json", "textmate/java/syntaxes/java.tmLanguage.json"))
                            2 -> editor.setEditorLanguage(getTextMateLanguage("json.tmLanguage.json", "textmate/json/syntaxes/json.tmLanguage.json"))
                            3 -> editor.setEditorLanguage(getTextMateLanguage("kotlin.tmLanguage.json", "textmate/kotlin/syntaxes/kotlin.tmLanguage"))
                            4 -> editor.setEditorLanguage(getTextMateLanguage("smali.tmLanguage.json", "textmate/smali/syntaxes/smali.tmLanguage.json"))
                            5 -> editor.setEditorLanguage(getTextMateLanguage("xml.tmLanguage.json", "textmate/xml/syntaxes/xml.tmLanguage.json"))
                            6 -> editor.setEditorLanguage(getTextMateLanguage("html.tmLanguage.json", "textmate/html/syntaxes/html.tmLanguage.json"))
                            7 -> editor.setEditorLanguage(getTextMateLanguage("javascript.tmLanguage.json", "textmate/javascript/syntaxes/JavaScript.tmLanguage.json"))
                            8 -> editor.setEditorLanguage(getTextMateLanguage("markdown.tmLanguage.json", "textmate/markdown/syntaxes/markdown.tmLanguage.json"))
                            9 -> editor.setEditorLanguage(getTextMateLanguage("python.tmLanguage.json", "textmate/python/syntaxes/python.tmLanguage.json"))
                            10 -> loadTMLLauncher.launch("*/*")
                            else -> editor.setEditorLanguage(EmptyLanguage())
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            R.id.search_panel_st -> {
                if (binding.searchPanel.visibility == View.GONE) {
                    binding.apply {
                        replaceEditor.setText("")
                        searchEditor.setText("")
                        editor.searcher.stopSearch()
                        searchPanel.visibility = View.VISIBLE
                        item.isChecked = true
                    }
                } else {
                    binding.searchPanel.visibility = View.GONE
                    editor.searcher.stopSearch()
                    item.isChecked = false
                }
            }
            R.id.switch_colors -> {
                val themes = arrayOf(
                    "Light",
                    "Dark",
                    "TM theme from file"
                )
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.color_scheme)
                    .setSingleChoiceItems(themes, -1) { dialog: DialogInterface, which: Int ->
                        when (which) {
                            0 -> editor.colorScheme = TextMateColorScheme(getLightTheme())
                            1 -> editor.colorScheme = TextMateColorScheme(getDarkTheme())
                            3 -> loadTMTLauncher.launch("*/*")
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            R.id.action_settings -> {
                navigator.navigateTo(
                    uiAction = UiAction("Settings_feature"),
                    onExtras = { intent ->
                        intent.putExtra("startUpTab", 2)
                    }
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showNavigationMethods() {
        mFilePath?.let { path ->
            CodeNavigationDialog(this).asyncShowPopup(
                this,
                path.path,
                binding.editor.text.toString()
            )
        }
    }

    override fun gotoLine(lineNO: Int) {
        binding.editor.jumpToLine(lineNO)
    }

    override fun onColorChange(argb: Int) {
        val strColor = String.format("#%08x", argb)
        copyToClipboard(this, strColor)
    }
}
