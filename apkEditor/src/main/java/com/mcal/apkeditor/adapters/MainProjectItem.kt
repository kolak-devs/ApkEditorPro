package com.mcal.apkeditor.adapters

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mcal.apkeditor.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

open class MainProjectItem() : AbstractItem<MainProjectItem.ViewHolder>() {
    var id: Int? = null
    var icon: Bitmap? = null
    var title: String? = null

    constructor(id: Int, icon: Bitmap? = null, title: String? = null) : this() {
        this.id = id
        this.icon = icon
        this.title = title
    }

    /** The type of the Item. Can be a hardcoded INT, but preferred is a defined id */
    override val type: Int
        get() = R.id.main_menu_container

    /** The layout for the given item */
    override val layoutRes: Int
        get() = R.layout.main_project_item

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<MainProjectItem>(view) {
        var icon: ImageView = view.findViewById(R.id.menu_icon)
        var title: TextView = view.findViewById(R.id.menu_title)

        /** Binds the data of this item onto the viewHolder */
        override fun bindView(item: MainProjectItem, payloads: List<Any>) {
            item.icon?.let {
                icon.setImageBitmap(it)
            }
            item.title?.let {
                title.text = it
            }
        }

        /** View needs to release resources when its recycled */
        override fun unbindView(item: MainProjectItem) {
            item.icon = null
            item.title = null
        }
    }
}