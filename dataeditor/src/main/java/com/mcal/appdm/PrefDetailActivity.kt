package com.mcal.appdm

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.Toast
import com.mcal.appdm.base.R
import com.mcal.appdm.base.databinding.AppdmActivityPrefdetailBinding
import com.mcal.appdm.utils.XmlUtils
import com.mcal.common.activities.CustomizedLangActivity
import com.mcal.common.utils.*
import com.mcal.common.utils.ActivityHelper.getBoolParam
import com.mcal.common.utils.ActivityHelper.getParam
import com.mcal.common.utils.ScopedStorage.getMyCp
import com.mcal.common.utils.ScopedStorage.getTempDir
import com.mcal.editor.TextEditor.getSoraEditor
import com.mcal.sqliteutil.util.PaddingTable
import com.mcal.sqliteutil.util.PaddingTable.ITableRowClicked
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.Unmodifiable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

class PrefDetailActivity : CustomizedLangActivity(), ITableRowClicked, View.OnClickListener {
    private val handler = MyHandler(this)

    // File path of the xml in /data/data/..
    private var filePath: String? = null

    // Temporary file path
    private var tmpFilePath: String? = null
    private var appName: String? = null
    private var thread: ParseThread? = null
    private var mKeyValues: LinkedHashMap<String?, Any?>? = null
    private var searchedKeyValues: LinkedHashMap<String?, Any?>? = null

    // all the data
    private var data: ArrayList<ArrayList<String?>> = ArrayList()

    // Is root mode or not
    private var isRootMode = false

    // Table View Wrapper
    private var paddingTable: PaddingTable? = null

    lateinit var binding: AppdmActivityPrefdetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AppdmActivityPrefdetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isRootMode = getBoolParam(intent, "isRootMode")
        appName = getParam(intent, "appName")
        filePath = getParam(intent, "xmlFilePath")
        initUI()
        refresh()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            1000 -> if (resultCode != 0) { // modified
                setResult(1)
                refresh()
            }
        }
    }

    fun refresh() {
        thread = ParseThread(this)
        thread?.start()
    }

    private fun initUI() {
        binding.tvAppname.text = appName
        filePath?.let {
            binding.tvPrefname.text = getShortPath(it)
        }
        binding.buttonSearch.visibility = View.GONE
        binding.buttonSearch.setOnClickListener(this)
        binding.btnRawFile.setOnClickListener(this)
    }

    private fun getShortPath(path: String): @Unmodifiable CharSequence {
        val pos = path.lastIndexOf("/")
        val filename = path.substring(pos + 1)
        // Delete ".xml"
        return filename.substring(0, filename.length - 4)
    }

    fun parseFinished(errMsg: String?) {
        if (errMsg == null) {
            handler.sendEmptyMessage(0)
        } else {
            handler.setErrorMessage(errMsg)
            handler.sendEmptyMessage(1)
        }
    }

    private fun prepareTable(keyValues: LinkedHashMap<String?, Any?>?) {
        if (keyValues == null) {
            return
        }
        mKeyValues = keyValues
        for (key in keyValues.keys) {
            val rowData = ArrayList<String?>()
            var value = ""
            keyValues[key]?.let { obj ->
                value = obj.toString()
            }
            rowData.add(key)
            rowData.add(value)
            data.add(rowData)
        }
        val header = ArrayList<String>()
        header.add("Key")
        header.add("Value")
        (paddingTable ?: PaddingTable(this, null, binding.valueTable, this).also { paddingTable = it }).apply {
            setTableHeaderNames(header)
            setTableData(data)
            prepareTable()
        }
    }

    private fun showTable() {
        paddingTable?.drawTable()
        // Show the result
        binding.scrollView.visibility = View.VISIBLE
        binding.buttonSearch.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    override fun tableRowClicked(position: Int, bWholeTable: Boolean) {
        // Now make it always editable
        if (!bWholeTable) {
            KeyValueDialog(this, searchedKeyValues, position, true)
        } else {
            KeyValueDialog(this, mKeyValues, position, true)
        }
    }

    @Throws(Exception::class)
    fun saveValue(key: String?, newValue: Any?) {
        mKeyValues?.set(key, newValue)
        if (isRootMode) {
            saveValueRoot()
        } else {
            saveValueNonRoot()
        }

        // Set as modified
        setResult(1)
    }

    @Throws(Exception::class)
    private fun saveValueRoot() {
        tmpFilePath?.let { tmp ->
            filePath?.let { file ->
                val out = FileOutputStream(tmpFilePath)
                XmlUtils.writeMapXml(mKeyValues, out)
                out.close()
                copyBack(tmp, file, isRootMode)
            }
        }
    }

    // For this mode, directly write to the file
    @Throws(Exception::class)
    private fun saveValueNonRoot() {
        val out = FileOutputStream(filePath)
        XmlUtils.writeMapXml(mKeyValues, out)
        out.close()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.button_close -> {
                binding.tvKeyword.setText("")
                doSearch()
            }
            R.id.button_search -> {
                doSearch()
            }
            R.id.btn_raw_file -> {
                // Open the editor
                tmpFilePath?.let { tmp ->
                    filePath?.let { file ->
                        val intent = getSoraEditor(this, tmp, file, isRootMode, intArrayOf(R.string.appdm_file_too_big, R.string.appdm_file_saved, R.string.appdm_not_found))
                        @Suppress("DEPRECATION")
                        startActivityForResult(intent, 1000)
                    }
                }
            }
        }
    }

    private fun doSearch() {
        mKeyValues?.let { keyValuesList ->
            val result = LinkedHashMap<String?, Any?>()
            var keyword = binding.tvKeyword.text.toString()
            keyword = keyword.trim()
            if (keyword == "") {
                result.putAll(keyValuesList)
            }

            val tableResult: MutableList<ArrayList<String?>> = ArrayList()
            for ((key, obj) in keyValuesList) {
                if (key != null) {
                    var matched = false

                    // Check the key
                    var value = key.lowercase(Locale.getDefault())
                    if (value.contains(keyword)) {
                        matched = true
                    }

                    // Check the value
                    if (!matched) {
                        if (obj != null) {
                            value = obj.toString().lowercase(Locale.getDefault())
                            if (value.contains(keyword)) {
                                matched = true
                            }
                        }
                    }
                    if (matched) {
                        result[key] = obj
                        val rowData = ArrayList<String?>()
                        rowData.add(key)
                        if (obj != null) {
                            rowData.add(obj.toString())
                        } else {
                            rowData.add("")
                        }
                        tableResult.add(rowData)
                    }
                }
            }
            if (result.isNotEmpty()) {
                searchedKeyValues = result
                paddingTable?.showSearchResult(tableResult)
            } else {
                Toast.makeText(this, "No record found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Suppress("DEPRECATION")
    private class MyHandler(activity: PrefDetailActivity) : Handler() {
        private var mActivity = activity
        private var message: String? = null

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0 -> {
                    mActivity.showTable()
                }
                1 -> Toast.makeText(mActivity, "Error: $message", Toast.LENGTH_LONG).show()
            }
        }

        fun setErrorMessage(errMsg: String?) {
            message = errMsg
        }
    }

    internal class ParseThread(activity: PrefDetailActivity) : Thread() {
        private val mActivity = activity

        override fun run() {
            var extraInfo: String? = null
            val activity = mActivity
            try {
                if (!exist()) {
                    throw Exception("Can not find SD Card!")
                }
                val workingDir = getTempDir().path
                val dir = File(workingDir)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val result: HashMap<String?, Any?>?
                if (activity.isRootMode) {
                    val f = File(activity.filesDir, "work.xml")
                    var tmpFilePath = f.path
                    activity.tmpFilePath = tmpFilePath
                    val rc = createCommandRunner(activity.isRootMode)
                    var strCmd = "cp"
                    val bin = getMyCp()
                    if (bin.exists()) {
                        strCmd = bin.path
                    }
                    val copyRet = rc.runCommand(
                        String.format("$strCmd \"%s\" %s", activity.filePath, tmpFilePath, tmpFilePath),
                        null, 2000
                    )
                    extraInfo = rc.stdError
                    // Copy file failed, try the original file
                    if (!copyRet) {
                        activity.filePath?.let {
                            tmpFilePath = it
                        }
                    }
                    val inputStream = FileInputStream(tmpFilePath)
                    result = XmlUtils.readMapXml(inputStream) as HashMap<String?, Any?>?
                    inputStream.close()
                } else {
                    val inputStream = FileInputStream(
                        activity.filePath
                    )
                    result = XmlUtils.readMapXml(inputStream) as HashMap<String?, Any?>?
                    inputStream.close()
                }

                val ret = LinkedHashMap<String?, Any?>()
                if (result != null) {
                    for ((key, value) in result) {
                        ret[key] = value
                    }
                }
                activity.prepareTable(ret)
                activity.parseFinished(null)
            } catch (e: Exception) {
                val errMsg = e.message + ": " + extraInfo
                activity.parseFinished(errMsg)
            }
        }
    }

    companion object {
        @Contract("_ -> new")
        private fun createCommandRunner(isRootMode: Boolean): CommandInterface {
            return if (isRootMode) {
                RootCommand()
            } else {
                CommandRunner()
            }
        }
    }
}