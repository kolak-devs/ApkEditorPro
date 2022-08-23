package com.mcal.neweditor.editor2.view

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout
import io.github.rosemoe.sora.widget.CodeEditor
import kotlin.math.max

class SymbolInputView : LinearLayout {
    private var mEditor: CodeEditor? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private fun init() {
        orientation = HORIZONTAL
    }

    fun bindEditor(editor: CodeEditor?) {
        this.mEditor = editor
    }

    fun setTextColor(color: Int) {
        for (i in 0 until childCount) {
            (getChildAt(i) as Button).setTextColor(color)
        }
    }

    fun removeSymbols() {
        removeAllViews()
    }

    fun addSymbols(display: Array<String?>, insertText: Array<String?>) {
        val count = max(display.size, insertText.size)
        for (i in 0 until count) {
            val btn = Button(context, null, android.R.attr.buttonStyleSmall)
            btn.text = display[i]
            btn.background = ColorDrawable(0)
            addView(btn, LayoutParams(-2, -1))
            btn.setOnClickListener {
                if (mEditor != null) mEditor!!.insertText(
                    insertText[i], 1
                )
            }
        }
    }
}