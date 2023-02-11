package com.mcal.editor.presentation

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.Cursor

class EditorViewModel(private val context: Context) : ViewModel() {

    private val _updatePositionText = MutableLiveData<String>()
    val updatePositionText: LiveData<String> = _updatePositionText

    private val _setupDiagnostics = MutableLiveData<DiagnosticsContainer>()
    val setupDiagnostics: LiveData<DiagnosticsContainer> = _setupDiagnostics

    fun setupDiagnostics(content: Content) {
        val container = DiagnosticsContainer()
        for (i in 0 until content.lineCount) {
            val index = content.getCharIndex(i, 0)
            container.addDiagnostic(
                DiagnosticRegion(
                    index,
                    index + content.getColumnCount(i),
                    DiagnosticRegion.SEVERITY_ERROR
                )
            )
        }
        _setupDiagnostics.value = container
    }

    fun updatePositionText(cursor: Cursor, content: Content) {
        var text = (1 + cursor.leftLine).toString() + ":" + cursor.leftColumn + " "
        text += if (cursor.isSelected) {
            "(" + (cursor.right - cursor.left) + " chars)"
        } else {
            if (content.getColumnCount(cursor.leftLine) == cursor.leftColumn) {
                "(<" + content.getLine(cursor.leftLine).lineSeparator.let {
                    if (it == io.github.rosemoe.sora.text.LineSeparator.NONE) {
                        "EOF"
                    } else {
                        it.name
                    }
                } + ">)"
            } else {
                "(" + escapeIfNecessary(
                    content.charAt(
                        cursor.leftLine,
                        cursor.leftColumn
                    )
                ) + ")"
            }
        }
        _updatePositionText.value = text
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
}
