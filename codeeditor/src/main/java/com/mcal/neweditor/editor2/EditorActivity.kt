package com.mcal.neweditor.editor2

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.neweditor.R
import com.mcal.neweditor.databinding.ActivitySoraeditorBinding
import com.mcal.neweditor.editor2.utils.FileUtils
import com.mcal.neweditor.editor2.widget.ProgressDialog
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.theme.TextMateColorScheme
import io.github.rosemoe.sora.textmate.core.internal.theme.reader.ThemeReader
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.component.Magnifier
import io.github.rosemoe.sora.widget.schemes.*
import io.github.rosemoe.sora.widget.style.builtin.ScaleCursorAnimator
import io.github.rosemoe.sorakt.subscribeEvent
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.api.JavaClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.regex.PatternSyntaxException

class EditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySoraeditorBinding
    private var save: MenuItem? = null
    private var undo: MenuItem? = null
    private var redo: MenuItem? = null
    private var smaliToJava: MenuItem? = null
    private var filePath: File? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySoraeditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val inputView = binding.symbolInput
        inputView.bindEditor(binding.editor)
        inputView.addSymbols(
            arrayOf(
                "->",
                "{",
                "}",
                "(",
                ")",
                ",",
                ".",
                ";",
                "\"",
                "?",
                "+",
                "-",
                "*",
                "/"
            ), arrayOf("\t", "{}", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/")
        )
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
                        // Regex error
                    }
                } else {
                    binding.editor.searcher.stopSearch()
                }
            }
        })
        binding.editor.apply {
            typefaceText = Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf")
            nonPrintablePaintingFlags =
                CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or CodeEditor.FLAG_DRAW_LINE_SEPARATOR or CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION
            // Update display dynamically
            subscribeEvent<SelectionChangeEvent> { _, _ -> updatePositionText() }
            subscribeEvent<ContentChangeEvent> { _, _ ->
                postDelayed(
                    ::updateBtnState,
                    50
                )
            }
        }

        // Custom cursor animator
        binding.editor.cursorAnimator =
            ScaleCursorAnimator(
                binding.editor
            )

        val editor = binding.editor
        var editorColorScheme = editor.colorScheme
        if (editorColorScheme !is TextMateColorScheme) {
            val iRawTheme = ThemeReader.readThemeSync(
                "QuietLight.tmTheme",
                assets.open("textmate/QuietLight.tmTheme")
            )
            editorColorScheme = TextMateColorScheme.create(iRawTheme)
            editor.colorScheme = editorColorScheme
        }
        val language: Language = TextMateLanguage.create(
            "java.tmLanguage.json",
            assets.open("textmate/java/syntaxes/java.tmLanguage.json"),
            InputStreamReader(assets.open("textmate/java/language-configuration.json")),
            (editorColorScheme as TextMateColorScheme).rawTheme
        )
        editor.setEditorLanguage(language)

        initIntent()
        openFile()
        updatePositionText()
        updateBtnState()
    }

    private fun initIntent() {
        filePath = File(intent.getStringExtra("xmlPath").toString())
    }

    private fun openFile() {
        Thread {
            try {
                val text: String = FileUtils.readFileDirectlyAsText(filePath!!.path)

                runOnUiThread {
                    supportActionBar!!.title= filePath!!.name
                    binding.editor.setText(text, null)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
        updatePositionText()
        updateBtnState()
    }

    @SuppressLint("FileEndsWithExt")
    private fun updateBtnState() {
        if (undo == null) {
            return
        }
        save!!.isEnabled = canSave()
        undo!!.isEnabled = binding.editor.canUndo()
        redo!!.isEnabled = binding.editor.canRedo()
        smaliToJava!!.isVisible = filePath!!.endsWith(".smali")
    }

    private fun canSave() : Boolean {
        return binding.editor.canUndo() || binding.editor.canRedo()
    }

    private fun updatePositionText() {
        val cursor = binding.editor.cursor
        var text = (1 + cursor.leftLine).toString() + ":" + cursor.leftColumn
        if (cursor.isSelected) {
            text += "(" + (cursor.right - cursor.left) + " chars)"
        }
        binding.positionDisplay.text = text
    }

    private val loadTMLLauncher = registerForActivityResult(GetContent()) { result: Uri? ->
        try {
            if (result == null) return@registerForActivityResult
            //TextMateLanguage only support TextMateColorScheme
            var editorColorScheme = binding.editor.colorScheme
            if (editorColorScheme !is TextMateColorScheme) {
                val iRawTheme = ThemeReader.readThemeSync(
                    "QuietLight.tmTheme",
                    assets.open("textmate/QuietLight.tmTheme")
                )
                editorColorScheme = TextMateColorScheme.create(iRawTheme)
                binding.editor.colorScheme = editorColorScheme
            }
            val language: Language = TextMateLanguage.create(
                result.path,
                contentResolver.openInputStream(result),
                (editorColorScheme as TextMateColorScheme).rawTheme
            )
            binding.editor.setEditorLanguage(language)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private val loadTMTLauncher = registerForActivityResult(GetContent()) { result: Uri? ->
        try {
            if (result == null) return@registerForActivityResult
            val iRawTheme = ThemeReader.readThemeSync(
                result.path, contentResolver.openInputStream(result)
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
        smaliToJava = menu.findItem(R.id.smali_to_java)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.editor.release()
    }

    fun save() {
        val dialog = ProgressDialog(this, "Saving", "Please wait...", false)

        dialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            filePath?.let { FileUtils.writeText(it.path, binding.editor.text.toString()) }

            withContext(Dispatchers.Main) {
                openFile()
                updatePositionText()
                updateBtnState()
                dialog.dismiss()
            }
        }
    }

    override fun onBackPressed() {
        if(canSave()) {
            val dialog = MaterialAlertDialogBuilder(this)
            dialog.setTitle(filePath!!.name)
            dialog.setMessage("Do you want to save this file?")
            dialog.setPositiveButton("Save") { _, _ ->
                save()
                super.onBackPressed()
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
        translate(filePath)
    }

    fun translate(smali: File?): String? {
        val args = JadxArgs()
        args.isSkipResources = true
        args.isShowInconsistentCode = true
        args.setInputFile(smali)

        val decompiler = JadxDecompiler(args)
        decompiler.load()

        val javaClass: JavaClass = decompiler.classes.iterator().next()
        javaClass.decompile()
        return javaClass.code
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val editor = binding.editor
        if (id == R.id.smali_to_java) {
            smaliToJava()
        } else if (id == R.id.text_save) {
            save()
        } else if (id == R.id.text_undo) {
            editor.undo()
        } else if (id == R.id.text_redo) {
            editor.redo()
        } else if (id == R.id.goto_end) {
            editor.setSelection(
                editor.text.lineCount - 1,
                editor.text.getColumnCount(editor.text.lineCount - 1)
            )
        } else if (id == R.id.move_up) {
            editor.moveSelectionUp()
        } else if (id == R.id.move_down) {
            editor.moveSelectionDown()
        } else if (id == R.id.home) {
            editor.moveSelectionHome()
        } else if (id == R.id.end) {
            editor.moveSelectionEnd()
        } else if (id == R.id.move_left) {
            editor.moveSelectionLeft()
        } else if (id == R.id.move_right) {
            editor.moveSelectionRight()
        } else if (id == R.id.magnifier) {
            item.isChecked = !item.isChecked
            editor.getComponent(Magnifier::class.java).isEnabled = item.isChecked
        } else if (id == R.id.code_format) {
            editor.formatCodeAsync()
        } else if (id == R.id.switch_language) {
            AlertDialog.Builder(this)
                .setTitle(R.string.switch_language)
                .setSingleChoiceItems(
                    arrayOf(
                        "Java",
                        "TextMate Java",
                        "TextMate Kotlin",
                        "TM Language from file",
                        "None"
                    ), -1
                ) { dialog: DialogInterface, which: Int ->
                    when (which) {
                        0 -> editor.setEditorLanguage(JavaLanguage())
                        1 -> try {
                            //TextMateLanguage only support TextMateColorScheme
                            var editorColorScheme = editor.colorScheme
                            if (editorColorScheme !is TextMateColorScheme) {
                                val iRawTheme = ThemeReader.readThemeSync(
                                    "QuietLight.tmTheme",
                                    assets.open("textmate/QuietLight.tmTheme")
                                )
                                editorColorScheme = TextMateColorScheme.create(iRawTheme)
                                editor.colorScheme = editorColorScheme
                            }
                            val language: Language = TextMateLanguage.create(
                                "java.tmLanguage.json",
                                assets.open("textmate/java/syntaxes/java.tmLanguage.json"),
                                InputStreamReader(assets.open("textmate/java/language-configuration.json")),
                                (editorColorScheme as TextMateColorScheme).rawTheme
                            )
                            editor.setEditorLanguage(language)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        2 -> try {
                            //TextMateLanguage only support TextMateColorScheme
                            var editorColorScheme = editor.colorScheme
                            if (editorColorScheme !is TextMateColorScheme) {
                                val iRawTheme = ThemeReader.readThemeSync(
                                    "QuietLight.tmTheme",
                                    assets.open("textmate/QuietLight.tmTheme")
                                )
                                editorColorScheme = TextMateColorScheme.create(iRawTheme)
                                editor.colorScheme = editorColorScheme
                            }
                            val language: Language = TextMateLanguage.create(
                                "Kotlin.tmLanguage",
                                assets.open("textmate/kotlin/syntaxes/Kotlin.tmLanguage"),
                                InputStreamReader(assets.open("textmate/kotlin/language-configuration.json")),
                                (editorColorScheme as TextMateColorScheme).rawTheme
                            )
                            editor.setEditorLanguage(language)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        3 -> loadTMLLauncher.launch("*/*")
                        else -> editor.setEditorLanguage(EmptyLanguage())
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else if (id == R.id.search_panel_st) {
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
        } else if (id == R.id.search_am) {
            binding.replaceEditor.setText("")
            binding.searchEditor.setText("")
            editor.searcher.stopSearch()
            editor.beginSearchMode()
        } else if (id == R.id.switch_colors) {
            val themes = arrayOf(
                "Default",
                "GitHub",
                "Eclipse",
                "Darcula",
                "VS2019",
                "NotepadXX",
                "QuietLight for TM",
                "Darcula for TM",
                "Abyss for TM",
                "TM theme from file"
            )
            AlertDialog.Builder(this)
                .setTitle(R.string.color_scheme)
                .setSingleChoiceItems(themes, -1) { dialog: DialogInterface, which: Int ->
                    when (which) {
                        0 -> editor.colorScheme = EditorColorScheme()
                        1 -> editor.colorScheme = SchemeGitHub()
                        2 -> editor.colorScheme = SchemeEclipse()
                        3 -> editor.colorScheme = SchemeDarcula()
                        4 -> editor.colorScheme = SchemeVS2019()
                        5 -> editor.colorScheme = SchemeNotepadXX()
                        6 -> try {
                            val iRawTheme = ThemeReader.readThemeSync(
                                "QuietLight.tmTheme",
                                assets.open("textmate/QuietLight.tmTheme")
                            )
                            val colorScheme = TextMateColorScheme.create(iRawTheme)
                            editor.colorScheme = colorScheme
                            val language = editor.editorLanguage
                            if (language is TextMateLanguage) {
                                language.updateTheme(iRawTheme)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        7 -> try {
                            val iRawTheme = ThemeReader.readThemeSync(
                                "darcula.json",
                                assets.open("textmate/darcula.json")
                            )
                            val colorScheme = TextMateColorScheme.create(iRawTheme)
                            editor.colorScheme = colorScheme
                            val language = editor.editorLanguage
                            if (language is TextMateLanguage) {
                                language.updateTheme(iRawTheme)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        8 -> try {
                            val iRawTheme = ThemeReader.readThemeSync(
                                "abyss-color-theme.json",
                                assets.open("textmate/abyss-color-theme.json")
                            )
                            val colorScheme = TextMateColorScheme.create(iRawTheme)
                            editor.colorScheme = colorScheme
                            val language = editor.editorLanguage
                            if (language is TextMateLanguage) {
                                language.updateTheme(iRawTheme)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        9 -> loadTMTLauncher.launch("*/*")
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else if (id == R.id.text_wordwrap) {
            item.isChecked = !item.isChecked
            editor.isWordwrap = item.isChecked
        } else if (id == R.id.editor_line_number) {
            editor.isLineNumberEnabled = !editor.isLineNumberEnabled
            item.isChecked = editor.isLineNumberEnabled
        } else if (id == R.id.pin_line_number) {
            editor.setPinLineNumber(!editor.isLineNumberPinned)
            item.isChecked = editor.isLineNumberPinned
        }
        return super.onOptionsItemSelected(item)
    }

    fun gotoNext(view: View?) {
        try {
            binding.editor.searcher.gotoNext()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun gotoLast(view: View?) {
        try {
            binding.editor.searcher.gotoPrevious()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun replace(view: View?) {
        try {
            binding.editor.searcher.replaceThis(binding.replaceEditor.text.toString())
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun replaceAll(view: View?) {
        try {
            binding.editor.searcher.replaceAll(binding.replaceEditor.text.toString())
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }
}