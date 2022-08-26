package com.mcal.apkeditor.adapters

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mcal.apkeditor.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

open class MainMenuItem(var id: Int, @DrawableRes var icon: Int? = null,
                        @StringRes var title: Int? = null) : AbstractItem<MainMenuItem.ViewHolder>() {


    /** The type of the Item. Can be a hardcoded INT, but preferred is a defined id */
    override val type: Int
        get() = R.id.main_menu_container

    /** The layout for the given item */
    override val layoutRes: Int
        get() = R.layout.main_list_item

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<MainMenuItem>(view){
        var icon = view.findViewById<ImageView>(R.id.menu_icon)
        var title = view.findViewById<TextView>(R.id.menu_title)

        /** Binds the data of this item onto the viewHolder */
        override fun bindView(item: MainMenuItem, payloads: List<Any>) {
            icon.setImageResource(item.icon!!)
            title.setText(item.title!!)
        }

        /** View needs to release resources when its recycled */
        override fun unbindView(item: MainMenuItem) {
            item.icon = null
            item.title = null
        }

    }

}