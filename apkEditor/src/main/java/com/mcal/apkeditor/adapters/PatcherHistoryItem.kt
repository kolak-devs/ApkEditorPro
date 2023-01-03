package com.mcal.apkeditor.adapters

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.mcal.apkeditor.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

class PatcherHistoryItem() : AbstractItem<PatcherHistoryItem.ViewHolder>() {
    var id: Int? = null
    var icon: Drawable? = null
    var title: String? = null
    var subtitle: String? = null;

    constructor(id: Int, icon: Drawable? = null, title: String? = null, subtitle: String? = null) : this() {
        this.id = id
        this.icon = icon
        this.title = title
        this.subtitle = subtitle
    }

    override val type: Int
        get() = R.id.patcher_menu_container

    override val layoutRes: Int
        get() = R.layout.item_patcher_list

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<PatcherHistoryItem>(view) {
        private var iconView: ImageView = view.findViewById(R.id.menu_icon)
        private var titleView: TextView = view.findViewById(R.id.menu_title)
        private var subtitleView: TextView = view.findViewById(R.id.menu_subtitle)

        override fun bindView(item: PatcherHistoryItem, payloads: List<Any>) {
            item.icon?.let {
                iconView.setImageDrawable(it)
            }
            item.title?.let { title ->
                titleView.text = title
            }
            item.subtitle.let { subtitle ->
                subtitleView.text = subtitle
            }
        }

        override fun unbindView(item: PatcherHistoryItem) {
            item.id = null
            item.icon = null
            item.title = null
            item.subtitle = null
        }
    }
}