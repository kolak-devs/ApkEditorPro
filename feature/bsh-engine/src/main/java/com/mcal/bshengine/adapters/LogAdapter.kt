package com.mcal.bshengine.adapters

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mcal.appdm.base.R
import com.mcal.common.data.Constants
import com.mikepenz.fastadapter.items.AbstractItem

open class LogAdapter : AbstractItem<LogAdapter.ViewHolder>() {
    private var logLevel: Int? = null
    private var logString: String? = null
    var bold: Boolean? = null

    override val type: Int
        get() = R.id.item_log

    override val layoutRes: Int
        get() = R.layout.item_bsh_log

    fun withId(id: Long): LogAdapter {
        this.identifier = id
        return this
    }

    fun withLogLevel(level: Int): LogAdapter {
        this.logLevel = level
        return this
    }

    fun withLogString(str: String): LogAdapter {
        this.logString = str
        return this
    }

    fun withBold(isBold: Boolean): LogAdapter {
        this.bold = isBold
        return this
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

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