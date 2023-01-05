package com.mcal.apkeditor.adapters

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.mcal.apkeditor.R
import com.mikepenz.fastadapter.items.AbstractItem

open class MainMenuItem() : AbstractItem<MainMenuItem.ViewHolder>() {
    @DrawableRes
    var icon: Int? = null
    var itemString: CharSequence? = null
    @StringRes
    var itemTitle: Int? = null

    /** The type of the Item. Can be a hardcoded INT, but preferred is a defined id */
    override val type: Int
        get() = R.id.main_menu_container

    /** The layout for the given item */
    override val layoutRes: Int
        get() = R.layout.item_main_list

    fun withId(id: Long): MainMenuItem {
        this.identifier = id
        return this
    }

    fun withIcon(icon: Int): MainMenuItem {
        this.icon = icon
        return this
    }

    fun withTitle(title: Int): MainMenuItem {
        this.itemTitle = title
        return this
    }

    fun withTitle(title: String): MainMenuItem {
        this.itemString = title
        return this
    }


    /** Binds the data of this item onto the viewHolder */
    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        icon?.let {
            holder.icon.setImageResource(it)
        }
        itemTitle?.let {
            holder.title.setText(it)
        }
        itemString?.let {
            holder.title.setText(it)
        }
    }

    /** View needs to release resources when its recycled */
    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.icon.setImageDrawable(null)
        holder.title.text = null
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var icon: ImageView = view.findViewById(R.id.menu_icon)
        var title: TextView = view.findViewById(R.id.menu_title)
    }
}