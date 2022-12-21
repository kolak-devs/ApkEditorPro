package com.mcal.apkeditor.adapters

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.mcal.apkeditor.R
import com.mcal.apkeditor.activities.ApkInfoExActivity
import com.mcal.common.utils.ActivityHelper.attachParam
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

open class MainProjectItem() : AbstractItem<MainProjectItem.ViewHolder>() {
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

    /** The type of the Item. Can be a hardcoded INT, but preferred is a defined id */
    override val type: Int
        get() = R.id.main_menu_container

    /** The layout for the given item */
    override val layoutRes: Int
        get() = R.layout.main_project_item

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(private val view: View) : FastAdapter.ViewHolder<MainProjectItem>(view) {
        var iconView: ImageView = view.findViewById(R.id.menu_icon)
        var titleView: TextView = view.findViewById(R.id.menu_title)
        var subtitleView: TextView = view.findViewById(R.id.menu_subtitle)

        /** Binds the data of this item onto the viewHolder */
        override fun bindView(item: MainProjectItem, payloads: List<Any>) {
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

        /** View needs to release resources when its recycled */
        override fun unbindView(item: MainProjectItem) {
            item.icon = null
            item.title = null
        }
    }
}