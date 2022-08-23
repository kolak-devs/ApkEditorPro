package com.mcal.apkeditor.colormixer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Parcelable
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.Spanned
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.mcal.apkeditor.R

class ColorMixer : RelativeLayout, TextWatcher {
    private var swatch: View? = null
    private var red: SeekBar? = null
    private var blue: SeekBar? = null
    private var green: SeekBar? = null
    private var alpha: SeekBar? = null
    private var colorEditText: EditText? = null
    private var mListener: OnColorChangedListener? = null

    // Record last time of value change
    private var valueFromText = 0
    private var valueFromBar = 0

    @SuppressLint("HandlerLeak")
    var hander = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == 0) {
                val color = color

                // Not set the text if it is from that
                if (color != valueFromText) {
                    //Log.d("DEBUG", "update color from progress bar: " + color);
                    setTextColorValue(color)
                }
                swatch?.setBackgroundColor(color)
                mListener?.onColorChange(color)
            }
        }
    }

    // Make changes from progress bar to text
    private val onMix = object : OnSeekBarChangeListener {
        override fun onProgressChanged(
            seekBar: SeekBar, progress: Int,
            fromUser: Boolean
        ) {
            hander.removeMessages(0)
            hander.sendEmptyMessageDelayed(0, 100)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
        }
    }

    constructor(context: Context?) : super(context) {
        initMixer(null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initMixer(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initMixer(attrs)
    }

    var onColorChangedListener: OnColorChangedListener?
        get() = mListener
        set(listener) {
            this.mListener = listener
        }

    val color: Int
        get() = Color.argb(alpha!!.progress, red!!.progress, green!!.progress, blue!!.progress)

    fun setProgressBarColor(color: Int) {
        red?.progress = Color.red(color)
        green?.progress = Color.green(color)
        blue?.progress = Color.blue(color)
        alpha?.progress = Color.alpha(color)

        // setTextColor(color);
    }

    private fun updateColorFromText() {
        colorEditText?.text.toString().let { strColor ->
            var value = strColor.toLong(16)

            // Text is updated by progress bar, do not update progress bar again
            if (value.toInt() == valueFromBar) {
                return
            }
            if (strColor.length <= 6) {
                value = value or -0x1000000
            }
            val color = value.toInt()
            valueFromText = color

            //Log.d("DEBUG", "update color from text: " + color);
            setProgressBarColor(color)
        }
    }

    private fun setTextColorValue(color: Int) {
        valueFromBar = color
        val text = Integer.toHexString(color)
        colorEditText?.setText(text)
        colorEditText?.setSelection(text.length)
    }

    private fun initMixer(attrs: AttributeSet?) {
        if (isInEditMode) {
            return
        }
        val inflater: LayoutInflater? = if (context is Activity) {
            (context as Activity).layoutInflater
        } else {
            LayoutInflater.from(context)
        }
        inflater?.inflate(R.layout.dialog_colormixer, this, true)
        swatch = findViewById(R.id.swatch)

        // Color EditText
        colorEditText = findViewById(R.id.color)
        colorEditText?.let { editText ->
            editText.addTextChangedListener(this)
            val filter = object : InputFilter {
                override fun filter(
                    source: CharSequence, start: Int, end: Int,
                    dest: Spanned, dstart: Int, dend: Int
                ): CharSequence? {
                    for (i in start until end) {
                        if (!isHexChar(source[i])) {
                            return ""
                        }
                    }
                    return null
                }

                private fun isHexChar(c: Char): Boolean {
                    if (c in '0'..'9') {
                        return true
                    }
                    return if (c in 'a'..'f') {
                        true
                    } else c in 'A'..'F'
                }
            }
            editText.filters = arrayOf(filter, LengthFilter(8))
        }
        red = findViewById(R.id.red)
        red?.let { redSeekbar ->
            redSeekbar.max = 0xFF
            redSeekbar.setOnSeekBarChangeListener(onMix)
        }
        green = findViewById(R.id.green)
        green?.let { greenSeekbar ->
            greenSeekbar.max = 0xFF
            greenSeekbar.setOnSeekBarChangeListener(onMix)
        }
        blue = findViewById(R.id.blue)
        blue?.let { blueSeekbar ->
            blueSeekbar.max = 0xFF
            blueSeekbar.setOnSeekBarChangeListener(onMix)
        }
        alpha = findViewById(R.id.alpha)
        alpha?.let { alphaSeekbar ->
            alphaSeekbar.max = 0xFF
            alphaSeekbar.setOnSeekBarChangeListener(onMix)
        }
    }

    public override fun onSaveInstanceState(): Parcelable {
        val state = Bundle()
        state.putParcelable(SUPERSTATE, super.onSaveInstanceState())
        state.putInt(COLOR, color)
        return state
    }

    public override fun onRestoreInstanceState(ss: Parcelable) {
        val state = ss as Bundle
        super.onRestoreInstanceState(state.getParcelable(SUPERSTATE))
        setProgressBarColor(state.getInt(COLOR))
    }

    override fun beforeTextChanged(
        s: CharSequence, start: Int, count: Int,
        after: Int
    ) {
    }

    override fun onTextChanged(
        s: CharSequence, start: Int, before: Int,
        count: Int
    ) {
    }

    // Make changes from text to progress bar
    override fun afterTextChanged(s: Editable) {
        try {
            updateColorFromText()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    interface OnColorChangedListener {
        fun onColorChange(argb: Int)
    }

    companion object {
        private const val SUPERSTATE = "superState"
        private const val COLOR = "color"
    }
}