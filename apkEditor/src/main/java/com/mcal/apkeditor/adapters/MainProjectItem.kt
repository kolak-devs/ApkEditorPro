package com.mcal.apkeditor.adapters

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.mcal.apkeditor.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

open class MainProjectItem : AbstractItem<MainProjectItem.ViewHolder>() {
    private var id: Int? = null
    private var icon: Drawable? = null
    private var title: String? = null
    private var subtitle: String? = null;

    /** The type of the Item. Can be a hardcoded INT, but preferred is a defined id */
    override val type: Int
        get() = R.id.main_menu_container

    /** The layout for the given item */
    override val layoutRes: Int
        get() = R.layout.item_main_project

    fun withId(id: Int): MainProjectItem {
        this.id = id
        return this
    }

    fun withIcon(icon: Drawable?): MainProjectItem {
        this.icon = icon
        return this
    }

    fun withTitle(title: String): MainProjectItem {
        this.title = title
        return this
    }

    fun withSubTitle(subtitle: String): MainProjectItem {
        this.subtitle = subtitle
        return this
    }

    fun getId(): Int? {
        return id
    }

    fun getTitle(): String? {
        return title
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<MainProjectItem>(view) {
        var iconView: ImageView = view.findViewById(R.id.menu_icon)
        var titleView: TextView = view.findViewById(R.id.menu_title)
        var subtitleView: TextView = view.findViewById(R.id.menu_subtitle)

        /** Binds the data of this item onto the viewHolder */
        override fun bindView(item: MainProjectItem, payloads: List<Any>) {
                iconView.setImageDrawable(item.icon)
                titleView.text = item.title
                subtitleView.text = item.subtitle
        }

        /** View needs to release resources when its recycled */
        override fun unbindView(item: MainProjectItem) {
            item.id = null
            item.icon = null
            item.title = null
            item.subtitle = null
        }
    }
}