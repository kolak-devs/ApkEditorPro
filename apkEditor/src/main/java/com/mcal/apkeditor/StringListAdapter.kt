package com.mcal.apkeditor

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mcal.apkeditor.activities.types.StringItem
import com.mcal.apkeditor.dialogs.StringValueDialog

class StringListAdapter(private val activity: Activity) : RecyclerView.Adapter<StringListAdapter.StringsListViewHolder>() {
    private val valueList: MutableList<StringItem> = ArrayList()

    // Record changed value
    private var changedValues: MutableMap<String, MutableMap<String, String>> = HashMap()

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

    class StringsListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.string_name)
        val value: TextView = itemView.findViewById(R.id.string_value)
    }

    init {
        mConfig = null
    }
}