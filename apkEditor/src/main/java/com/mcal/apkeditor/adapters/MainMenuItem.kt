package com.mcal.apkeditor.adapters

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mcal.apkeditor.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

open class MainMenuItem() : AbstractItem<MainMenuItem.ViewHolder>() {
    var id: Int? = null
    @DrawableRes
    private var icon: Int? = null
    private var itemTitle: CharSequence? = null
    @StringRes
    private var itemString: Int? = null

    constructor(id: Int, icon: Int?, title: CharSequence): this(){
        this.id = id
        this.icon = icon
        this.itemTitle = title
    }

    constructor(id: Int, icon: Int?, title: Int): this(){
        this.id = id
        this.icon = icon
        this.itemString = title
    }

    /** The type of the Item. Can be a hardcoded INT, but preferred is a defined id */
    override val type: Int
        get() = R.id.main_menu_container

    /** The layout for the given item */
    override val layoutRes: Int
        get() = R.layout.item_main_list

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<MainMenuItem>(view) {
        var icon: ImageView = view.findViewById(R.id.menu_icon)
        var title: TextView = view.findViewById(R.id.menu_title)

        /** Binds the data of this item onto the viewHolder */
        override fun bindView(item: MainMenuItem, payloads: List<Any>) {
            item.icon?.let {
                icon.setImageResource(it)
            }
            item.itemTitle?.let {
                title.setText(it)
            }
            item.itemString?.let {
                title.setText(it)
            }
        }

        /** View needs to release resources when its recycled */
        override fun unbindView(item: MainMenuItem) {
            item.id = null
            item.icon = null
            item.itemTitle = null
        }
    }
}