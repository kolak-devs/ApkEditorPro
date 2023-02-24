package com.mcal.bshengine.adapters

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mcal.bshengine.R
import com.mikepenz.fastadapter.items.AbstractItem

open class LogAdapter : AbstractItem<LogAdapter.ViewHolder>() {
    var itemTitle: String? = null

    override val type: Int
        get() = R.id.main_menu_container

    override val layoutRes: Int
        get() = R.layout.item_bsh_log

    fun withId(id: Long): LogAdapter {
        this.identifier = id
        return this
    }

    fun withTitle(title: String): LogAdapter {
        this.itemTitle = title
        return this
    }

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        itemTitle?.let {
            holder.title.text = it
        }
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.title.text = null
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var title: TextView = view.findViewById(R.id.menu_title)
    }
}