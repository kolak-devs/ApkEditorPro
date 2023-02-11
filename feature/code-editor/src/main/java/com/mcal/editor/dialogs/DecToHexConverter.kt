package com.mcal.editor.dialogs

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.text.*
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.neweditor.R
import com.mcal.neweditor.databinding.DialogDec2hexConverterBinding

class DecToHexConverter(private val activity: Activity) : View.OnTouchListener {
    private lateinit var binding: DialogDec2hexConverterBinding
    private lateinit var watcherDec: TextWatcher
    private lateinit var watcherHex: TextWatcher
    private lateinit var btnNegative: Button

    fun show() {
        binding = DialogDec2hexConverterBinding.inflate(activity.layoutInflater)
        watcherDec = WatcherText(this, TYPE_DEC)
        watcherHex = WatcherText(this, TYPE_HEX)

        val inputDec = binding.inputDec
        inputDec.filters = arrayOf(FilterInput(this, "[0-9-]+"), InputFilter.LengthFilter(20))
        inputDec.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        inputDec.addTextChangedListener(watcherDec)
        inputDec.setOnTouchListener(this)
        inputDec.setSelection(inputDec.length())

        val inputHex = binding.inputHex
        inputHex.filters = arrayOf(FilterInput(this, "[0-9a-fA-Fx-]+"), InputFilter.LengthFilter(19))
        inputHex.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        inputHex.addTextChangedListener(watcherHex)
        inputHex.setOnTouchListener(this)

        val dialog = MaterialAlertDialogBuilder(activity)
        dialog.setTitle(activity.getString(R.string.dec2hex))
        dialog.setView(binding.root)
        dialog.setPositiveButton(activity.getString(R.string.close), null)
        dialog.setNegativeButton(activity.getString(R.string.clear)) { _, _ ->
            binding.inputDec.setText("")
            binding.inputHex.setText("")
        }
        val alert = dialog.create()
        alert.show()
        alert.getButton(DialogInterface.BUTTON_NEGATIVE).apply {
            isEnabled = false
        }.also {
            btnNegative = it
        }
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            val i = motionEvent.x.toInt()
            getTouchView(view, binding.inputDec, i)
            getTouchView(view, binding.inputHex, i)
        }
        return false
    }

    private fun getTouchView(view: View, input: EditText?, x: Int) {
        if (view === input) {
            if (x >= input.width - input.compoundPaddingRight && x < input.width) {
                val cm = activity.getSystemService(ClipboardManager::class.java)
                val cd = ClipData.newPlainText("copied", input.text.toString())
                cm.setPrimaryClip(cd)
                if (Build.VERSION.SDK_INT < 33) {
                    Toast.makeText(activity, activity.getString(R.string.copied_to_clipboard) + input.text.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun textListener(index: Int) {
        val id = activity.resources.getIdentifier("text_color_primary", "color", "android")
        val defaultColor = activity.getColor(id)
        val inputDec = binding.inputDec
        val inputHex = binding.inputHex
        inputDec.setTextColor(defaultColor)
        inputHex.setTextColor(defaultColor)
        if (index != TYPE_DEC) {
            inputDec.removeTextChangedListener(watcherDec)
            try {
                if (inputHex.text.toString().isEmpty()) {
                    inputDec.setText("")
                } else {
                    inputDec.setText(inputHex.text.toString().replace("0x".toRegex(), "").toLong(16).toString(10))
                }
            } catch (e: Exception) {
                inputDec.setText("")
                inputHex.setTextColor(Color.RED)
            }
            inputDec.addTextChangedListener(watcherDec)
        }
        if (index != TYPE_HEX) {
            inputHex.removeTextChangedListener(watcherHex)
            try {
                if (inputDec.text.toString().isEmpty()) {
                    inputHex.setText("")
                } else {
                    val value = inputDec.text.toString().toLong(10)
                    val result: String = if (value < 0) {
                        "-0x" + (value * -1).toString(16)
                    } else {
                        "0x" + value.toString(16)
                    }
                    inputHex.setText(result)
                }
            } catch (e: Exception) {
                inputHex.setText("")
                inputDec.setTextColor(Color.RED)
            }
            inputHex.addTextChangedListener(watcherHex)
        }
        btnNegative.isEnabled = inputDec.text.toString().isNotEmpty() || inputHex.text.toString().isNotEmpty()
        val icCopy = ContextCompat.getDrawable(activity, R.drawable.ic_copy)
        inputDec.setCompoundDrawablesWithIntrinsicBounds(null, null, if (inputDec.length() > 0) icCopy else null, null)
        inputHex.setCompoundDrawablesWithIntrinsicBounds(null, null, if (inputHex.length() > 0) icCopy else null, null)
    }

    class WatcherText(private val converter: DecToHexConverter, private val index: Int) : TextWatcher {
        override fun afterTextChanged(s: Editable) {}
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            getWatcher(converter, index)
        }
    }

    inner class FilterInput(private val converter: DecToHexConverter, private val regex: String) : InputFilter {
        override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence {
            return if (source.toString().matches(Regex(regex))) source else ""
        }
    }

    companion object {
        const val TYPE_DEC = 0
        const val TYPE_HEX = 1
        fun getWatcher(converter: DecToHexConverter, index: Int) {
            converter.textListener(index)
        }
    }
}