package com.mcal.apkeditor.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mcal.apkeditor.R
import com.mcal.apkeditor.activities.types.StringItem
import com.mcal.apkeditor.dialogs.StringValueDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class StringListAdapter(
    private val activity: Activity,
    private val listener: StringItemListener
) : RecyclerView.Adapter<StringListAdapter.StringsListViewHolder>() {
    private var valueList: MutableList<StringItem> = ArrayList()

    private val dataBackup: MutableList<StringItem> = ArrayList()

    // Record changed value
    private var changedValues: MutableMap<String, MutableMap<String, String>> = HashMap()

    @JvmField
    var newValue: String? = null

    @JvmField
    var canStartFilterProcess = true

    // Current configuration (which language)
    private var mConfig: String?
    override fun getItemCount(): Int {
        synchronized(valueList) {
            return valueList.size
        }
    }

    fun getItem(position: Int): Any {
        synchronized(valueList) {
            return valueList[position]
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StringsListViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_stringvalue_static, parent, false)
        return StringsListViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StringsListViewHolder, position: Int) {
        var value: StringItem
        synchronized(valueList) {
            value = valueList[position]
            holder.name.text = value.name
            holder.value.text = value.value

            holder.value.setOnClickListener {
                val dialog = StringValueDialog(activity, this, position)
                synchronized(valueList) {
                    val item = valueList[position]
                    dialog.setKeyValue(item.name, item.value)
                }
            }
        }
    }

    // Update a new display
    fun updateData(config: String?, list: List<StringItem>) {
        synchronized(valueList) {
            this.mConfig = config
            valueList.clear()
            valueList.addAll(list)
            dataBackup.addAll(list)
        }
        notifyDataSetChanged()
    }

    fun checkTextChange(position: Int, newValue: String) {
        var valueChanged = false
        synchronized(valueList) {
            if (position >= 0 && position < valueList.size) {
                val item = valueList[position]
                if (item.value != newValue) {
                    mConfig?.let { config ->
                        // Here will change the original value (allStringValues in ApkInfoActivity)
                        item.value = newValue
                        var valueMap = changedValues[config]
                        if (valueMap == null) {
                            valueMap = HashMap()
                            changedValues[config] = valueMap
                        }
                        valueMap[item.name] = newValue
                    }
                    valueChanged = true
                    // LOGGER.info(pair.m1 + " changed value: " + value);
                }
            }
        }
        if (valueChanged) {
            notifyDataSetChanged()
        }
    }

    fun getChangedValues(): Map<String, Map<String, String>> {
        return changedValues
    }

    fun setChangedValues(changedStringValues: MutableMap<String, MutableMap<String, String>>) {
        changedValues = changedStringValues
    }

    fun filter(constraint: CharSequence?) = CoroutineScope(Dispatchers.IO).launch {
        val startResultList = mutableListOf<StringItem>()
        val resultList = mutableListOf<StringItem>()
        val endResultList = mutableListOf<StringItem>()
        val charSearch = constraint.toString().lowercase(Locale.ROOT)
        if (charSearch.isEmpty()) {
            valueList = dataBackup
        } else {
            loop@ for (row in dataBackup) {
                val name = row.value
                var index = name.indexOf(charSearch)
                if (index == 0) {
                    startResultList.add(row)
                } else if (index > 0) {
                    do {
                        if (name[index - 1] == ' ') {
                            resultList.add(row)
                            continue@loop
                        }
                        index = name.indexOf(charSearch, index + 1)
                    } while (index > 0)
                    endResultList.add(row)
                } else if (name.contains(charSearch)) {
                    endResultList.add(row)
                }
            }
            val offset1 = startResultList.size
            val offset2 = resultList.size
            val length = offset1 + offset2 + endResultList.size
            val list: MutableList<StringItem> = java.util.ArrayList(length)
            for (app in startResultList) {
                list.add(0, app)
            }
            for (app in resultList) {
                list.add(offset1, app)
            }
            for (app in endResultList) {
                list.add(offset1 + offset2, app)
            }
            valueList = list
        }
        CoroutineScope(Dispatchers.Main).launch {
            publishResults(valueList)
        }
    }

    interface StringItemListener {
        fun onFoundStrings(mode: Boolean)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun publishResults(results: MutableList<StringItem>?) {
        if (results != null) {
            val length = results.size
            listener.onFoundStrings(length > 0)
            if (length >= 0) {
                valueList = results
                notifyDataSetChanged()
            }
        }
        val text = newValue
        if (text.isNullOrEmpty()) {
            canStartFilterProcess = true
            return
        }
        newValue = null
        filter(text)
    }

    class StringsListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.string_name)
        val value: TextView = itemView.findViewById(R.id.string_value)
    }

    init {
        mConfig = null
    }
}