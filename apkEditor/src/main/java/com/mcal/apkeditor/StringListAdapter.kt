package com.mcal.apkeditor

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.TextView
import com.mcal.apkeditor.dialogs.StringValueDialog
import common.types.StringItem

class StringListAdapter(activity: Activity) : BaseAdapter(), OnItemClickListener {
    private val valueList: MutableList<StringItem> = ArrayList()
    private val mActivity: Activity

    // Record changed value
    private var changedValues: MutableMap<String, MutableMap<String, String>> = HashMap()

    // Current configuration (which language)
    private var mConfig: String?
    override fun getCount(): Int {
        synchronized(valueList) { return valueList.size }
    }

    override fun getItem(position: Int): Any {
        synchronized(valueList) { return valueList[position] }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view: View
        var value: StringItem
        synchronized(valueList) {
            value = valueList[position]
            view = LayoutInflater.from(mActivity).inflate(R.layout.item_stringvalue_static, null)
            val viewHolder = ViewHolder()
            viewHolder.name = view.findViewById(R.id.string_name)
            viewHolder.value = view.findViewById(R.id.string_value)
            view.tag = viewHolder
            viewHolder.name?.text = value.name
            viewHolder.value?.text = value.value
        }
        return view
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

    override fun onItemClick(
        arg0: AdapterView<*>?, arg1: View, position: Int,
        id: Long
    ) {
        val dlg = StringValueDialog(mActivity, this, position)
        synchronized(valueList) {
            val item = valueList[position]
            dlg.setKeyValue(item.name, item.value)
        }
    }

    private class ViewHolder {
        var name: TextView? = null
        var value: TextView? = null
    }

    init {
        mActivity = activity
        mConfig = null
    }
}