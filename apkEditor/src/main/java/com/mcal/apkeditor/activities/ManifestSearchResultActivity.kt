package com.mcal.apkeditor.activities

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.mcal.apkeditor.R
import com.mcal.common.activities.CustomizedLangActivity
import java.io.*

class ManifestSearchResultActivity : CustomizedLangActivity(), View.OnClickListener {
    private val editViews = ArrayList<EditText>()
    private var xmlPath: String? = null
    private var lineIndexes: ArrayList<Int>? = null
    private var lineContents: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_manifest_search_result)
        intent.extras?.let { bundle ->
            xmlPath = bundle.getString("filePath")
            lineIndexes = bundle.getIntegerArrayList("lineIndexs")
            lineContents = bundle.getStringArrayList("lineContents")
        }
        initView()
    }

    private fun initView() {
        val format = resources.getString(R.string.mf_search_ret)
        lineIndexes?.size?.let { length ->
            val title = String.format(format, length)
            setupToolbar(id = R.id.toolbar, title = title, back = true)
        }
        val saveBtn = findViewById<View>(R.id.btn_save) as Button
        saveBtn.setOnClickListener(this)
        val closeBtn = findViewById<View>(R.id.btn_close) as Button
        closeBtn.setOnClickListener(this)
        val layout = findViewById<View>(R.id.result_layout) as LinearLayout
        lineContents?.let { contents ->
            for (i in contents.indices) {
                val et = EditText(this)
                et.setText(contents[i])
                et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                layout.addView(et)
                editViews.add(et)
            }
        }
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.btn_close) {
            finish()
        } else if (id == R.id.btn_save) {
            saveModification()
        }
    }

    private fun saveModification() {
        var modified = false
        val views = editViews
        val contents = lineContents
        // Collect the modification
        for (i in views.indices) {
            val et = views[i]
            val newStr = et.text.toString()
            contents?.get(i)?.let { oldStr ->
                if (oldStr != newStr) {
                    contents[i] = newStr
                    modified = true
                }
            }
        }
        if (modified) {
            if (saveManifest()) {
                Toast.makeText(this, R.string.succeed, Toast.LENGTH_SHORT).show()
                // To indicate the manifest is modified
                setResult(1)
                finish()
            }
        } else {
            Toast.makeText(this, R.string.no_change_detected, Toast.LENGTH_SHORT).show()
        }
    }

    // The value is already collected before calling
    private fun saveManifest(): Boolean {
        var succeed = false
        try {
            xmlPath?.let { path ->
                val fos = FileOutputStream("$path.tmp")
                val fis = FileInputStream(path)
                val br = BufferedReader(InputStreamReader(fis))

                // Read all the contents
                val allContents: MutableList<String> = ArrayList()
                var line = br.readLine()
                while (line != null) {
                    allContents.add(line)
                    line = br.readLine()
                }
                lineIndexes?.let { indexes ->
                    // Revise the content
                    for (i in indexes.indices) {
                        val lineIndex = indexes[i] - 1
                        lineContents?.get(i)?.let { newStr ->
                            val oldStr = allContents[lineIndex]
                            val head = getHeadPadding(oldStr)
                            allContents[lineIndex] = head + newStr.trim()
                        }
                    }
                }

                // Save the new content
                for (lineStr in allContents) {
                    fos.write(lineStr.toByteArray())
                    fos.write('\n'.code)
                }
                br.close()
                fis.close()
                fos.close()

                // Move temp file to overwrite the origin file
                File("$path.tmp").renameTo(File(path))
                succeed = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
        return succeed
    }

    // Get the head blanks
    private fun getHeadPadding(str: String): String {
        val sb = StringBuilder()
        for (element in str) {
            if (element == ' ' || element == '\t') {
                sb.append(element)
            } else {
                break
            }
        }
        return sb.toString()
    }
}