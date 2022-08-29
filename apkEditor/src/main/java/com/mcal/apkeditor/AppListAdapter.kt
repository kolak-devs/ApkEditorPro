package com.mcal.apkeditor

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import java.util.*

class AppListAdapter(private val ctx: Context) : BaseAdapter() {
    private val pm: PackageManager = ctx.packageManager

    var appList = mutableListOf<AppInfo>()
    private var order: AppListOrder? = null
    fun setAppList(appList: MutableList<AppInfo>, order: String) {
        val orderConst = ctx.resources.getStringArray(
            R.array.order_value
        )
        when (order) {
            orderConst[0] -> {
                this.order = AppListOrder.BY_NAME
            }
            orderConst[1] -> {
                this.order = AppListOrder.BY_INSTALL_TIME
            }
            else -> {
                this.order = AppListOrder.BY_NAME
            }
        }

        sortAppList(appList)
        synchronized(this.appList) {
            this.appList.clear()
            this.appList.addAll(appList)
        }
    }

    private fun sortAppList(appList: MutableList<AppInfo>) {
        val locale = Locale.getDefault()
        var comparator: Comparator<AppInfo>
        order?.let { appListOrder ->
            comparator = when (appListOrder) {
                AppListOrder.BY_NAME -> {
                    Comparator.comparing { arg0: AppInfo -> arg0.appName.lowercase(locale) }
                }
                AppListOrder.BY_INSTALL_TIME -> {
                    Comparator { arg0: AppInfo, arg1: AppInfo -> if (arg0.lastUpdateTime < arg1.lastUpdateTime) 1 else -1 }
                }
            }
            appList.sortWith(comparator)
        }
    }

    override fun getCount(): Int {
        synchronized(appList) {
            return appList.size
        }
    }

    override fun getItem(arg0: Int): Any {
        synchronized(appList) {
            return appList[arg0]
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("InflateParams", "ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val appInfo = getItem(position) as AppInfo
        val view = LayoutInflater.from(ctx).inflate(R.layout.item_applist, null)
        val viewHolder = ViewHolder()
        viewHolder.icon = view.findViewById(R.id.app_icon)
        viewHolder.appName = view.findViewById(R.id.app_name)
        viewHolder.desc1 = view.findViewById(R.id.app_desc1)
        viewHolder.desc2 = view.findViewById(R.id.app_desc2)
        view.tag = viewHolder
        try {
            viewHolder.appName?.text = appInfo.appName
            viewHolder.desc1?.text = appInfo.packagePath
            var icon = appInfo.icon
            if (icon == null) {
                icon = appInfo.applicationInfo.loadIcon(pm)
            }
            viewHolder.icon?.setImageDrawable(icon)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return view
    }

    private enum class AppListOrder {
        BY_NAME, BY_INSTALL_TIME
    }

    internal class ViewHolder {
        var icon: ImageView? = null
        var desc2: TextView? = null
        var desc1: TextView? = null
        var appName: TextView? = null
    }
}