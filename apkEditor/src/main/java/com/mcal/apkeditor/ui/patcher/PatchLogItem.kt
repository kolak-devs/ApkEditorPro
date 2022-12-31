package com.mcal.apkeditor.ui.patcher

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import com.mcal.apkeditor.R
import com.mcal.common.data.Constants

import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

open class PatchLogItem() : AbstractItem<PatchLogItem.ViewHolder>() {
    var logLevel: Int? = null
    var logString: String? = null
    var bold: Boolean = false

    constructor(logLevel: Int, logString: String? = null, bold: Boolean = false) : this() {
        this.logLevel = logLevel
        this.logString = logString
        this.bold = bold
    }

    /** The type of the Item. Can be a hardcoded INT, but preferred is a defined id */
    override val type: Int
        get() = R.id.item_log

    /** The layout for the given item */
    override val layoutRes: Int
        get() = R.layout.item_patchlog

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<PatchLogItem>(view) {
        var content: TextView? = null

        /** Binds the data of this item onto the viewHolder */
        override fun bindView(item: PatchLogItem, payloads: List<Any>) {
            content = itemView as TextView
            content?.apply {
                if (item.logLevel == Constants.LOG_ERROR){
                    this.setTextColor(Color.RED)
                }
                if (item.bold){
                    this.typeface = Typeface.DEFAULT_BOLD
                }
                this.text = item.logString
            }
        }

        /** View needs to release resources when its recycled */
        override fun unbindView(item: PatchLogItem) {
            item.logLevel = null
            item.logString = null
        }
    }
}