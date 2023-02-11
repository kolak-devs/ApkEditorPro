package com.mcal.colorconverter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mcal.colormixer.R
import com.mcal.colormixer.databinding.DialogColorConverterBinding

class ColorPickerConverter : View.OnTouchListener, DialogInterface.OnClickListener, View.OnClickListener {
    private val activity: Activity
    private var alpha = 0
    private var red = 0
    private var green = 0
    private var blue = 0
    private var callback: ColorPickerCallback? = null
    private lateinit var materialDialog: AlertDialog
    private lateinit var watcherArgb: TextWatcher
    private lateinit var watcherSmali: TextWatcher
    private lateinit var watcherJava: TextWatcher
    private lateinit var btnPositive: Button
    private lateinit var btnNegative: Button
    private lateinit var btnNeutral: Button
    private lateinit var binding: DialogColorConverterBinding

    constructor(activity: Activity) {
        this.activity = activity
    }

    constructor(activity: Activity, color: Int) {
        this.activity = activity
        alpha = Color.alpha(color)
        red = Color.red(color)
        green = Color.green(color)
        blue = Color.blue(color)
    }

    constructor(activity: Activity, alpha: Int, red: Int, green: Int, blue: Int) {
        this.activity = activity
        this.alpha = channelRange(alpha)
        this.red = channelRange(red)
        this.green = channelRange(green)
        this.blue = channelRange(blue)
    }

    private fun channelRange(colorValue: Int): Int {
        return if (colorValue in 0..255) colorValue else 0
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        binding = DialogColorConverterBinding.inflate(activity.layoutInflater)
        
        watcherArgb = WatcherText(this, 1)
        watcherSmali = WatcherText(this, 2)
        watcherJava = WatcherText(this, 3)
        val inputArgb = binding.inputHexArgb
        val inputSmali = binding.inputHexSmali
        val inputJava = binding.inputIntJava
        if (alpha != 0 || red != 0 || green != 0 || blue != 0) {
            inputArgb.setText(
                String.format(
                    "#%02x%02x%02x%02x",
                    channelRange(alpha),
                    channelRange(red),
                    channelRange(green),
                    channelRange(blue)
                )
            )
            inputSmali.setText(Convert.fromArgbToSmali(inputArgb.text.toString()))
            inputJava.setText(Convert.fromArgbToJava(inputArgb.text.toString()))
            binding.colorView.background = getBackgroundColorView(color)
        } else {
            binding.colorView.background = getBackgroundColorView(0)
        }
        for (etArr in arrayOf(inputArgb, inputSmali, inputJava)) {
            etArr.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        inputArgb.filters = arrayOf(FilterInput("[a-fA-F0-9#]+"), InputFilter.LengthFilter(9))
        inputSmali.filters = arrayOf(FilterInput("[a-fA-F0-9-x]+"), InputFilter.LengthFilter(11))
        inputJava.filters = arrayOf(FilterInput("[0-9-]+"), InputFilter.LengthFilter(11))
        inputArgb.addTextChangedListener(watcherArgb)
        inputSmali.addTextChangedListener(watcherSmali)
        inputJava.addTextChangedListener(watcherJava)
        inputArgb.setOnTouchListener(this)
        inputSmali.setOnTouchListener(this)
        inputJava.setOnTouchListener(this)

        inputArgb.setSelection(inputArgb.length())

        val builder = MaterialAlertDialogBuilder(activity)
        builder.setTitle(R.string.color_converter)
        builder.setView(binding.root)
        builder.setPositiveButton(android.R.string.ok, this)
        builder.setNegativeButton(R.string.clear, null)
        builder.setNeutralButton("-0x", null)

        materialDialog = builder.create()
        materialDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        materialDialog.show()

        btnPositive = materialDialog.getButton(DialogInterface.BUTTON_POSITIVE).also { it.isAllCaps = false }
        btnNegative = materialDialog.getButton(DialogInterface.BUTTON_NEGATIVE).also { it.isAllCaps = false }
        btnNeutral = materialDialog.getButton(DialogInterface.BUTTON_NEUTRAL).also { it.isAllCaps = false }

        stateButtons()

        btnNegative.setOnClickListener(this)
        btnNeutral.setOnClickListener(this)
    }

    fun setCallback(listener: ColorPickerCallback?) {
        callback = listener
    }

    @SuppressLint("SetTextI18n")
    override fun onClick(view: View) {
        val inputSmali = binding.inputHexSmali
        when (view.id) {
            android.R.id.button2 -> for (etArr in arrayOf(binding.inputHexArgb, inputSmali, binding.inputIntJava)) {
                etArr.setText("")
            }
            android.R.id.button3 -> {
                inputSmali.setText("-0x")
                inputSmali.setSelection(inputSmali.length())
                inputSmali.requestFocus()
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(inputSmali, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        try {
            callback?.onColorSelected(color)
        } catch (e: Exception) {
            Toast.makeText(activity, R.string.invalid_color_value, Toast.LENGTH_SHORT).show()
        }
    }

    private fun textListener(index: Int, str: String) {
        val inputSmali = binding.inputHexSmali
        val inputJava = binding.inputIntJava
        val inputArgb = binding.inputHexArgb
        for (etArr in arrayOf(inputArgb, inputSmali, inputJava)) {
            etArr.setTextColor(textColorPrimary)
        }
        if (index == 1) {
            inputSmali.removeTextChangedListener(watcherSmali)
            try {
                convert(inputSmali, str, Convert.fromArgbToSmali(str))
            } catch (e: Exception) {
                convertError(inputArgb, inputSmali)
            }
            inputSmali.addTextChangedListener(watcherSmali)
            inputJava.removeTextChangedListener(watcherJava)
            try {
                convert(inputJava, str, Convert.fromArgbToJava(str))
            } catch (e: Exception) {
                convertError(inputArgb, inputJava)
            }
            inputJava.addTextChangedListener(watcherJava)
        }
        if (index == 2) {
            inputArgb.removeTextChangedListener(watcherArgb)
            try {
                convert(inputArgb, str, Convert.fromSmaliToArgb(str))
            } catch (e: Exception) {
                convertError(inputSmali, inputArgb)
            }
            inputArgb.addTextChangedListener(watcherArgb)
            inputJava.removeTextChangedListener(watcherJava)
            try {
                convert(inputJava, str, Convert.fromSmaliToJava(str))
            } catch (e: Exception) {
                convertError(inputSmali, inputJava)
            }
            inputJava.addTextChangedListener(watcherJava)
        }
        if (index == 3) {
            inputArgb.removeTextChangedListener(watcherArgb)
            try {
                convert(inputArgb, str, Convert.fromJavaToArgb(str))
            } catch (e: Exception) {
                convertError(inputJava, inputArgb)
            }
            inputArgb.addTextChangedListener(watcherArgb)
            inputSmali.removeTextChangedListener(watcherSmali)
            try {
                convert(inputSmali, str, Convert.fromJavaToSmali(str))
            } catch (e: Exception) {
                convertError(inputJava, inputSmali)
            }
            inputSmali.addTextChangedListener(watcherSmali)
        }
        stateButtons()
        try {
            binding.colorView.background = getBackgroundColorView(color)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val icCopy = ContextCompat.getDrawable(activity, R.drawable.cp_ic_copy)
        icCopy?.setTint(textColorPrimary)
        for (etArr in arrayOf(inputArgb, inputSmali, inputJava)) {
            etArr.setCompoundDrawablesWithIntrinsicBounds(null, null, if (etArr.length() > 0) icCopy else null, null)
        }
    }

    private fun convert(output: EditText, str: String, convert: String) {
        if (str.isEmpty()) {
            binding.colorView.background = getBackgroundColorView(0)
            output.setText("")
        } else {
            output.setText(convert)
        }
    }

    private fun convertError(input: EditText, output: EditText) {
        binding.colorView.background = getBackgroundColorView(0)
        output.setText("")
        input.setTextColor(Color.RED)
    }

    private fun stateButtons() {
        val textHexSmali = binding.inputHexSmali.text.toString()
        btnNegative.isEnabled = binding.inputHexArgb.text.toString().isNotEmpty() || textHexSmali.isNotEmpty() || !binding.inputIntJava.text.toString().isEmpty()
        btnNeutral.isEnabled = !textHexSmali.contains("-0x")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            val i = motionEvent.x.toInt()
            getTouchView(view, binding.inputHexArgb, i)
            getTouchView(view, binding.inputHexSmali, i)
            getTouchView(view, binding.inputIntJava, i)
        }
        return false
    }

    private fun getTouchView(view: View, input: EditText, x: Int) {
        if (view === input) {
            if (x >= input.width - input.compoundPaddingRight && x < input.width) {
                val cm = activity.getSystemService(ClipboardManager::class.java)
                val cd = ClipData.newPlainText("copied", input.text.toString())
                cm.setPrimaryClip(cd)
                if (Build.VERSION.SDK_INT < 33) {
                    Toast.makeText(activity, activity.resources.getString(R.string.copied_to_clipboard, input.text.toString()), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getBackgroundColorView(color: Int): Drawable {
        val drawable = GradientDrawable()
        drawable.setColor(color)
        drawable.setStroke((activity.resources.displayMetrics.density + 0.5f).toInt(), Color.GRAY)
        drawable.cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10.0f, activity.resources.displayMetrics)
        return drawable
    }

    private val textColorPrimary: Int
        get() {
            val res = activity.resources
            @SuppressLint("DiscouragedApi") val id = res.getIdentifier("text_color_primary", "color", "android")
            return activity.getColor(id)
        }
    private val color: Int
        get() {
            val color = binding.inputIntJava.text.toString()
            return color.toInt()
        }

    companion object {
        @JvmStatic
        fun getWatcher(cp: ColorPickerConverter, index: Int, str: String) {
            cp.textListener(index, str)
        }
    }
}