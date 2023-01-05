package com.mcal.apkeditor.ui.patcher

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mcal.apkeditor.R
import com.mcal.common.data.Constants
import com.mikepenz.fastadapter.items.AbstractItem

open class PatchLogItem() : AbstractItem<PatchLogItem.ViewHolder>() {
    private var logLevel: Int? = null
    private var logString: String? = null
    var bold: Boolean? = null

    /** The type of the Item. Can be a hardcoded INT, but preferred is a defined id */
    override val type: Int
        get() = R.id.item_log

    /** The layout for the given item */
    override val layoutRes: Int
        get() = R.layout.item_patchlog

    fun withId(id: Long): PatchLogItem {
        this.identifier = id
        return this
    }

    fun withLogLevel(level: Int): PatchLogItem {
        this.logLevel = level
        return this
    }

    fun withLogString(str: String): PatchLogItem {
        this.logString = str
        return this
    }

    fun withBold(isBold: Boolean): PatchLogItem {
        this.bold = isBold
        return this
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    /** Binds the data of this item onto the viewHolder */
    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.content.apply {
            if (logLevel == Constants.LOG_ERROR) {
                this.setTextColor(Color.RED)
            }
            if (bold == true) {
                this.typeface = Typeface.DEFAULT_BOLD
            }
            this.text = logString
        }
    }

    /** View needs to release resources when its recycled */
    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.content.apply {
            this.text = null
            this.typeface = null
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var content = view as TextView
    }
}