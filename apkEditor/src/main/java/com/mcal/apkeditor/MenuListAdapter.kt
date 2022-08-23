package com.mcal.apkeditor

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import java.lang.ref.WeakReference

class MenuListAdapter(ctx: Context) : BaseAdapter() {
    private val weakReference: WeakReference<Context>

    init {
        weakReference = WeakReference(ctx)
    }

    override fun getCount(): Int {
        return titles.size
    }

    override fun getItem(i: Int): Any {
        return titles[i]
    }

    override fun getItemId(i: Int): Long {
        return itemIds[i].toLong()
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View? {
        var view = convertView
        val viewHolder: ViewHolder
        if (view != null) {
            viewHolder = view.tag as ViewHolder
        } else {
            view = LayoutInflater.from(weakReference.get()).inflate(R.layout.item_main_menu, null)
            viewHolder = ViewHolder()
            viewHolder.iconIv = view.findViewById(R.id.menu_icon)
            viewHolder.titleTv = view.findViewById(R.id.menu_title)
            view.tag = viewHolder
        }
        viewHolder.iconIv?.setImageResource(drawables[i])
        viewHolder.titleTv?.setText(titles[i])
        return view
    }

    private class ViewHolder {
        var iconIv: ImageView? = null
        var titleTv: TextView? = null
    }

    companion object {
        const val ITEM_PROJECT = 0
        const val ITEM_FORUM = 1
        const val ITEM_SETTING = 2
        const val ITEM_ABOUT = 3
        const val ITEM_IMG_DOWNLOADER = 4
        const val ITEM_TELEGRAM = 5
        const val ITEM_LOGS = 6

        // Google play version
        private val titles = intArrayOf(
            R.string.projects, R.string.settings, R.string.image_downloader, R.string.about,
            R.string.view_logs, R.string.link_forum, R.string.link_telegram
        )
        private val drawables = intArrayOf(
            R.drawable.round_inventory_2_24,
            R.drawable.round_settings_24,
            R.drawable.round_image_24,
            R.drawable.round_info_24,
            R.drawable.round_logo_dev_24,
            R.drawable.outline_link_24,
            R.drawable.outline_link_24
        )
        private val itemIds = intArrayOf(
            ITEM_PROJECT,
            ITEM_SETTING,
            ITEM_IMG_DOWNLOADER,
            ITEM_ABOUT,
            ITEM_LOGS,
            ITEM_FORUM,
            ITEM_TELEGRAM
        )
    }
}