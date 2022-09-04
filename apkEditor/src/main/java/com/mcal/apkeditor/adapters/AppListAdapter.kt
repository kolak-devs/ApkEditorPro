package com.mcal.apkeditor.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mcal.apkeditor.AppInfo
import com.mcal.apkeditor.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class AppListAdapter(
    context: Context,
    private val appList: MutableList<AppInfo>,
    private val listener: AppItemClick
) :
    RecyclerView.Adapter<AppListAdapter.AppListViewHolder>() {
    var newValue: String? = null
    var canStartFilterProcess = true
    private val pm: PackageManager = context.packageManager
    private var appFilterList = appList

    interface AppItemClick {
        fun onClick(item: AppInfo)
        fun onLongClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppListViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_applist, parent, false)
        return AppListViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
        val appInfo = appFilterList[position]
        holder.appName.text = appInfo.appName
        holder.desc1.text = appInfo.packagePath
        holder.icon.loadImage(appInfo)
        holder.itemView.setOnClickListener {
            listener.onClick(appInfo)
        }
        holder.itemView.setOnLongClickListener {
            listener.onLongClick(position)
            return@setOnLongClickListener true
        }
    }

    private fun ImageView.loadImage(appInfo: AppInfo) = CoroutineScope(Dispatchers.IO).launch {
        var icon = appInfo.icon
        if (icon == null) {
            icon = appInfo.applicationInfo.loadIcon(pm)
        }
        CoroutineScope(Dispatchers.Main).launch {
            this@loadImage.setImageDrawable(icon)
        }
    }

    override fun getItemCount(): Int {
        return appFilterList.size
    }

    class AppListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.app_icon)
        val desc1: TextView = itemView.findViewById(R.id.app_desc1)
        val appName: TextView = itemView.findViewById(R.id.app_name)
    }

    fun filter(constraint: CharSequence?) = CoroutineScope(Dispatchers.IO).launch {
        val charSearch = constraint.toString()
        appFilterList = if (charSearch.isEmpty()) {
            appList
        } else {
            val resultList = mutableListOf<AppInfo>()
            for (row in appList) {
                if (row.appName.lowercase(Locale.ROOT).contains(charSearch.lowercase(Locale.ROOT))
                ) {
                    resultList.add(row)
                }
            }
            resultList
        }
        CoroutineScope(Dispatchers.Main).launch {
            publishResults(appFilterList)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun publishResults(results: MutableList<AppInfo>?) {
        if (results != null) {
            val length = results.size
            if (length > 0) {
                appFilterList = results
                notifyDataSetChanged()
            }
        }
        val text = newValue
        if (text.isNullOrEmpty()) {
            canStartFilterProcess = true
            return
        }
        newValue = null
        filter(text)
    }
}