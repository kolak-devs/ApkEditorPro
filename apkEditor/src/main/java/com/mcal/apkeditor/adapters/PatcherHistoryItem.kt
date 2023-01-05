package com.mcal.apkeditor.adapters

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mcal.apkeditor.R
import com.mikepenz.fastadapter.items.AbstractItem

class PatcherHistoryItem : AbstractItem<PatcherHistoryItem.ViewHolder>() {
    var icon: Drawable? = null
    var title: String? = null
    var subtitle: String? = null;

    override val type: Int
        get() = R.id.patcher_menu_container

    override val layoutRes: Int
        get() = R.layout.item_patcher_list

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.iconView.setImageDrawable(icon)
        holder.titleView.text = title
        holder.subtitleView.text = subtitle
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.iconView.setImageDrawable(null)
        holder.titleView.text = null
        holder.subtitleView.text = null
    }

    fun withId(id: Long): PatcherHistoryItem {
        this.identifier = id
        return this
    }

    fun withIcon(icon: Drawable?): PatcherHistoryItem {
        this.icon = icon
        return this
    }

    fun withTitle(title: String): PatcherHistoryItem {
        this.title = title
        return this
    }

    fun withSubTitle(subtitle: String): PatcherHistoryItem {
        this.subtitle = subtitle
        return this
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var iconView: ImageView = view.findViewById(R.id.menu_icon)
        var titleView: TextView = view.findViewById(R.id.menu_title)
        var subtitleView: TextView = view.findViewById(R.id.menu_subtitle)
    }
}