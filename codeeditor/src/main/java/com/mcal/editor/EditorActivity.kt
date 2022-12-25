package com.mcal.editor

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.apkeditor.colormixer.ColorMixer
import com.mcal.colormixer.ColorMixerDialog
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.data.ReactivePreferences
import com.mcal.common.utils.ClipboardUtils.copyToClipboard
import com.mcal.common.utils.ScopedStorage
import com.mcal.common.utils.copyBack
import com.mcal.common.view.ProgressDialog
import com.mcal.editor.dialogs.SmaliCodeDialog
import com.mcal.editor.navigation.CodeNavigationDialog
import com.mcal.editor.utils.FileUtils
import com.mcal.editor.utils.JavaExtractor
import com.mcal.neweditor.R
import com.mcal.neweditor.databinding.ActivitySoraeditorBinding
import io.github.rosemoe.sora.event.*
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.text.LineSeparator
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.component.Magnifier
import io.github.rosemoe.sora.widget.style.builtin.ScaleCursorAnimator
import io.github.rosemoe.sora.widget.subscribeEvent
import jadx.api.JadxDecompiler
import jadx.plugins.input.smali.SmaliInputPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.tm4e.core.registry.IGrammarSource
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.PatternSyntaxException


class EditorActivity : CustomizedLangActivity(),
    CodeNavigationDialog.ISmaliMethodClicked, ColorMixer.OnColorChangedListener {
    private lateinit var binding: ActivitySoraeditorBinding

    private var save: MenuItem? = null
    private var undo: MenuItem? = null
    private var redo: MenuItem? = null
    private var dexToJava: MenuItem? = null
    private var smaliToJava: MenuItem? = null
    private var methodsList: MenuItem? = null
    private var templatesMenu: MenuItem? = null

    private var filePath: File? = null
    private var apkPath: File? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySoraeditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(R.id.toolbar, "Editor", false)
        initIntent()
        getFileName()
        initSymbolsList()
        initSearchEditor()
        initEditor()
        openFile()
        updatePositionText()
        updateBtnState()
        setupDiagnostics()
    }

    private fun initEditor() {
        val editor = binding.editor
        editor.apply {
            lineSeparator = LineSeparator.CRLF
            typefaceText = Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf")
            setLineSpacing(2f, 1.1f)
            // Update display dynamically
            subscribeEvent<SelectionChangeEvent> { _, _ -> updatePositionText() }
            subscribeEvent<ContentChangeEvent> { _, _ ->
                postDelayed(::updateBtnState, 50)
            }
            subscribeEvent<SideIconClickEvent> { _, _ ->
                Toast.makeText(this@EditorActivity, "Side icon clicked", Toast.LENGTH_SHORT).show()
            }

            subscribeEvent<KeyBindingEvent> { event, _ ->
                if (event.eventType != EditorKeyEvent.Type.DOWN) {
                    return@subscribeEvent
                }

                Toast.makeText(
                    context,
                    "Keybinding event: " + generateKeybindingString(event),
                    Toast.LENGTH_LONG
                ).show()
            }
            // Custom cursor animator
            cursorAnimator = ScaleCursorAnimator(editor)
            typefaceText = Typeface.MONOSPACE

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

    private fun setupDiagnostics() {
        val editor = binding.editor
        val container = DiagnosticsContainer()
        for (i in 0 until editor.text.lineCount) {
            val index = editor.text.getCharIndex(i, 0)
            container.addDiagnostic(
                DiagnosticRegion(
                    index,
                    index + editor.text.getColumnCount(i),
                    DiagnosticRegion.SEVERITY_ERROR
                )
            )
        }
        editor.diagnostics = container
    }

    private fun generateKeybindingString(event: KeyBindingEvent): String {
        val sb = StringBuilder()
        if (event.isCtrlPressed) {
            sb.append("Ctrl + ")
        }

        if (event.isAltPressed) {
            sb.append("Alt + ")
        }

        if (event.isShiftPressed) {
            sb.append("Shift + ")
        }

        sb.append(KeyEvent.keyCodeToString(event.keyCode))
        return sb.toString()
    }

    private fun updatePositionText() {
        val cursor = binding.editor.cursor
        var text = (1 + cursor.leftLine).toString() + ":" + cursor.leftColumn + " "
        text += if (cursor.isSelected) {
            "(" + (cursor.right - cursor.left) + " chars)"
        } else {
            val content = binding.editor.text
            if (content.getColumnCount(cursor.leftLine) == cursor.leftColumn) {
                "(<" + content.getLine(cursor.leftLine).lineSeparator.let {
                    if (it == LineSeparator.NONE) {
                        "EOF"
                    } else {
                        it.name
                    }
                } + ">)"
            } else {
                "(" + escapeIfNecessary(
                    binding.editor.text.charAt(
                        cursor.leftLine,
                        cursor.leftColumn
                    )
                ) + ")"
            }
        }
        binding.positionDisplay.text = text
    }

    private fun escapeIfNecessary(c: Char): String {
        return when (c) {
            '\n' -> "\\n"
            '\t' -> "\\t"
            '\r' -> "\\r"
            ' ' -> "<ws>"
            else -> c.toString()
        }
    }

    private fun initSearchEditor() {
        binding.searchEditor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (editable.isNotEmpty()) {
                    try {
                        binding.editor.searcher.search(
                            editable.toString(),
                            EditorSearcher.SearchOptions(true, true)
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
        filePath?.name?.let { fileName ->
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
        filePath = File(intent.getStringExtra("filePath").toString())
        curFileIndex = intent.getIntExtra("curFileIndex", 0)
        startLine = intent.getIntExtra("startLine", 0)
        startLineList = intent.getIntegerArrayListExtra("startLineList")
        apkPath = File(intent.getStringExtra("apkPath").toString())
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
            filePath?.let { path ->
                copyBack(path.path, realPath, isRootMode)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun openFile() {
        filePath?.let { path ->
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
            updatePositionText()
            updateBtnState()
        }
    }

    private fun getFileName() {
        filePathList?.let { list ->
            filePath = File(list[curFileIndex])
        }
        realFilePath?.let { path ->
            filePath = File(path)
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
        filePath?.let { path ->
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
                        filePath?.let {
                            FileUtils.writeText(it.path, binding.editor.text.toString())
                        }

                        realFilePath?.let { path ->
                            copyBack2RealPath(path)
                        }
                        withContext(Dispatchers.Main) {
                            openFile()
                            updatePositionText()
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
            filePath?.name?.let { name ->
                dialog.setTitle(name)
            }
            dialog.setMessage("Do you want to save this file?")
            dialog.setPositiveButton("Save") { _, _ ->
                save(true)
            }
            dialog.setNegativeButton("Don't save") { _, _ ->
                super.onBackPressed()
            }
            dialog.show()
        } else {
            super.onBackPressed()
        }
    }

    private fun smaliToJava() {
        val inflater = LayoutInflater.from(this)
        val view: View = inflater.inflate(R.layout.dialog_proccessing, null)
        val dialog: AlertDialog = MaterialAlertDialogBuilder(this).create()
        dialog.setView(view)
        dialog.setCancelable(false)
        dialog.show()

        var javaPath: String? = null
        CoroutineScope(Dispatchers.IO).launch {
            filePath?.let { smaliPath ->
                JadxDecompiler().use { decompiler ->
                    decompiler.addCustomLoad(
                        SmaliInputPlugin().loadFiles(
                            listOf<Path>(
                                Paths.get(
                                    smaliPath.path
                                )
                            )
                        )
                    )
                    decompiler.load()
                    for (cls in decompiler.classes) {
                        val packageNamePath = File(
                            ScopedStorage.getTmpDir().path + File.separator + cls.getPackage().replace(".", "/")
                        )
                        if (!packageNamePath.exists()) {
                            packageNamePath.mkdirs()
                        }
                        javaPath = packageNamePath.toString() + File.separator + cls.name + ".java"
                        javaPath?.let { path ->
                            FileUtils.writeText(path, cls.code)
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                javaPath?.let { path ->
                    val intent = TextEditor.getSoraEditor(this@EditorActivity, path, null, 0, null)
                    startActivity(intent)
                    dialog.dismiss()
                }
            }
        }
    }

    private fun dexToJava() {
        if (apkPath == null) {
            Toast.makeText(
                this,
                "Internal error: cannot find apk path to decode java code, please contact the author.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val workingDirectory: String = try {
            ScopedStorage.getTmpDir().path
        } catch (e: java.lang.Exception) {
            Toast.makeText(this, "Cannot make working directory.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            return
        }

        val dexAndClass = getDexAndClassName()
        val dexName = dexAndClass[0]
        val className = dexAndClass[1]

        val inflater = LayoutInflater.from(this)
        val view: View = inflater.inflate(R.layout.dialog_proccessing, null)

        val dialog = MaterialAlertDialogBuilder(this).create()
        dialog.setView(view)
        dialog.setCancelable(false)
        dialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            apkPath?.let { path ->
                val extractor = JavaExtractor(path.path, dexName, className, workingDirectory)
                val succeed = extractor.extract()
                var errMessage: String? = null
                if (!succeed) {
                    errMessage = extractor.errorMessage
                }

                withContext(Dispatchers.Main) {
                    if (succeed) {
                        var relativePath = className.substring(1)
                        var filePath = "$workingDirectory/$relativePath.java"
                        var fileExist = File(filePath).exists()
                        if (!fileExist) {
                            do {
                                // Try to remove string after $
                                val position = relativePath.lastIndexOf('$')
                                if (position != -1) {
                                    relativePath = relativePath.substring(0, position)
                                    filePath = "$workingDirectory/$relativePath.java"
                                    fileExist = File(filePath).exists()
                                    if (fileExist) {
                                        break
                                    }
                                }

                                // Try to get the file in defpackage folder
                                filePath = workingDirectory + File.separator + "defpackage/" + relativePath + ".java"
                                fileExist = File(filePath).exists()
                                if (fileExist) {
                                    break
                                }
                            } while (false)
                        }
                        if (!fileExist) {
                            Toast.makeText(
                                this@EditorActivity,
                                "Cannot find java file",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            val intent: Intent =
                                TextEditor.getSoraEditor(
                                    this@EditorActivity,
                                    filePath,
                                    null,
                                    0,
                                    null
                                )
                            startActivity(intent)
                        }
                    } else {
                        Toast.makeText(this@EditorActivity, errMessage, Toast.LENGTH_LONG).show()
                    }
                    dialog.dismiss()
                }
            }
        }
    }

    private fun getDexAndClassName(): Array<String> {
        var dexName = "classes.dex"
        val sb = StringBuilder()
        filePath?.path?.split("/")?.toTypedArray()?.let { dirs ->
            var i = 0
            while (i < dirs.size) {
                if ("smali" == dirs[i]) {
                    break
                }
                if (dirs[i].startsWith("smali_")) {
                    dexName = dirs[i].substring(6) + ".dex"
                    break
                }
                i++
            }
            sb.append('L')
            i += 1
            while (i < dirs.size) {
                var name = dirs[i]
                if (i == dirs.size - 1) {
                    if (name.length > 6 && name.endsWith(".smali")) {
                        name = name.substring(0, name.length - 6)
                        sb.append(name)
                    }
                } else {
                    sb.append(name)
                    sb.append('/')
                }
                i++
            }
        }
        return if (sb.isEmpty()) emptyArray() else arrayOf(dexName, sb.toString())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val editor = binding.editor
        when (id) {
            R.id.pallete -> {
                ColorMixerDialog(this, 0xFFFFFF, this)
            }
            R.id.methods -> {
                showNavigationMethods()
            }
            R.id.template -> {
                filePath?.let {
                    SmaliCodeDialog(this, it.path)
                }
            }
            R.id.smali_to_java -> {
                smaliToJava()
            }
            R.id.dex_to_java -> {
                dexToJava()
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
                try {
                    startActivity(Intent(
                        this,
                        Class.forName("com.mcal.apkeditor.activities.SettingsActivity")
                    ).apply {
                        putExtra("startUpTab", 2)
                    })
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("UNUSED_PARAMETER")
    fun gotoNext(view: View?) {
        try {
            binding.editor.searcher.gotoNext()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun gotoLast(view: View?) {
        try {
            binding.editor.searcher.gotoPrevious()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun replace(view: View?) {
        try {
            binding.editor.searcher.replaceThis(binding.replaceEditor.text.toString())
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun replaceAll(view: View?) {
        try {
            binding.editor.searcher.replaceAll(binding.replaceEditor.text.toString())
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    private fun showNavigationMethods() {
        filePath?.let { path ->
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