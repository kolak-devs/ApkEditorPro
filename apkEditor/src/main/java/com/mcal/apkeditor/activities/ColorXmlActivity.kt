package com.mcal.apkeditor.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import com.google.android.material.appbar.MaterialToolbar
import com.mcal.apkeditor.R
import com.mcal.apkeditor.colormixer.ColorMixer
import com.mcal.apkeditor.colormixer.ColorValue
import com.mcal.apkeditor.colormixer.ColorValueAdapter
import com.mcal.colormixer.ColorMixerDialog
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.utils.ActivityHelper
import com.mcal.editor.TextEditor.getSoraEditor
import java.io.*


class ColorXmlActivity : CustomizedLangActivity(), View.OnClickListener, OnItemClickListener {
    private var xmlPath: String? = null
    private var colorValues: ArrayList<ColorValue>? = null
    private var colorAdapter: ColorValueAdapter? = null
    private var saveBtn: Button? = null
    private var closeBtn: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_colors_xml)
        xmlPath = ActivityHelper.getParam(intent, "filePath")
        initData()
        initView()
        setupToolbar(fileName)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_color_xml, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_edit_code -> {
                openInEditor()
            }
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0) {
            if (resultCode != 0) {
                initData()
                updateView()
                setResult()
            }
        }
    }

    private fun initView() {
        colorAdapter = ColorValueAdapter(this, colorValues)
        val colorList = findViewById<ListView>(R.id.color_list)
        colorList.adapter = colorAdapter
        colorList.onItemClickListener = this
        saveBtn = findViewById(R.id.btn_save)
        saveBtn?.setOnClickListener(this)
        closeBtn = findViewById(R.id.btn_close)
        closeBtn?.setOnClickListener(this)
    }

    private fun setupToolbar(title: String) {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.let { actionBar ->
            actionBar.title = title
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
        }
    }

    private fun updateView() {
        colorAdapter?.updateData(colorValues)
    }

    private val fileName: String
        get() {
            val pos = xmlPath!!.lastIndexOf('/')
            return xmlPath!!.substring(pos + 1)
        }

    private fun initData() {
        colorValues = ArrayList()
        colorValues?.let { colorList ->
            var br: BufferedReader? = null
            try {
                br = BufferedReader(FileReader(xmlPath))
                var line = br.readLine()
                while (line != null) {
                    parseLine(line)?.let { value ->
                        colorList.add(value)
                    }
                    line = br.readLine()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    br?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            // Parse the reference value
            for (i in colorList.indices) {
                colorList[i].parseRefColor(this, colorList)
            }
        }
    }

    // Parse a line like: <color name="white">#ffffffff</color>
    private fun parseLine(line: String): ColorValue? {
        var result: ColorValue? = null
        val tag = "<color name=\""
        val position = line.indexOf(tag)
        if (position != -1) {
            var startPos = position + tag.length
            var endPos = line.indexOf("\">", startPos)
            if (endPos != -1) {
                val name = line.substring(startPos, endPos)
                startPos = endPos + 2
                endPos = line.indexOf("</color>", startPos)
                if (endPos != -1) {
                    val strValue = line.substring(startPos, endPos)
                    result = ColorValue(name, strValue)
                }
            }
        }
        return result
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_close -> {
                finish()
            }
            R.id.btn_save -> {
                saveColors()
                finish()
            }
        }
    }

    private fun openInEditor() {
        xmlPath?.let { path ->
            val intent = getSoraEditor(this, path, null, 0, null)
            ActivityHelper.attachParam(intent, "extraString", ENTRY_NAME)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, 0)
        }
    }

    private fun saveColors() {
        var bw: BufferedWriter? = null
        try {
            bw = BufferedWriter(FileWriter(xmlPath))
            bw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            bw.write("<resources>\n")
            for (i in colorValues!!.indices) {
                bw.write(colorValues!![i].toString())
                bw.write("\n")
            }
            bw.write("</resources>")
            setResult()
        } catch (e: Exception) {
            val fmt = getString(R.string.general_error)
            val message = String.format(fmt, e.message)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } finally {
            if (bw != null) {
                try {
                    bw.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Make parent activity aware the modification
    private fun setResult() {
        // Set modified flag as the result
        val intent = Intent()
        intent.putExtra("filePath", xmlPath)
        intent.putExtra("extraString", ENTRY_NAME)
        this.setResult(1, intent)
    }

    override fun onItemClick(
        parent: AdapterView<*>?, view: View?,
        position: Int, id: Long
    ) {
        colorValues?.get(position)?.intColorValue?.let { color ->
            ColorMixerDialog(this, color,
                object : ColorMixer.OnColorChangedListener {
                    override fun onColorChange(argb: Int) {
                        colorChanged(position, argb)
                    }
                })
        }
    }

    // The color is changed through the color dialog
    private fun colorChanged(position: Int, argb: Int) {
        colorValues?.let { colorList ->
            if (position < colorList.size) {
                val value = colorList[position]
                value.intColorValue = argb
                value.strColorValue = "#" + Integer.toHexString(argb)
                saveBtn?.let { save -> save.visibility = View.VISIBLE }
                colorAdapter!!.notifyDataSetChanged()
            }
        }
    }

    companion object {
        private const val ENTRY_NAME = "res/values/colors.xml"
    }
}