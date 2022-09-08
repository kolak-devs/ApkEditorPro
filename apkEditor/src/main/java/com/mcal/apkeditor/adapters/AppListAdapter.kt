package com.mcal.apkeditor.adapters

import android.annotation.SuppressLint
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
import java.util.regex.Matcher
import java.util.regex.Pattern


class AppListAdapter(
    private val pm: PackageManager,
    private val appList: List<AppInfo>,
    private val listener: AppItemClick
) :
    RecyclerView.Adapter<AppListAdapter.AppListViewHolder>() {
    var newValue: String? = null
    var canStartFilterProcess = true
    private var appFilterList = appList
    private var mMatcher: Matcher? = null
    private var mStringBuffer: StringBuffer? = null

    interface AppItemClick {
        fun onClick(item: AppInfo)
        fun onLongClick(position: Int)
        fun onFoundApp(mode: Boolean)
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
        val startResultList = mutableListOf<AppInfo>()
        val resultList = mutableListOf<AppInfo>()
        val endResultList = mutableListOf<AppInfo>()
        val charSearch = constraint.toString().lowercase(Locale.ROOT)
        if (charSearch.isEmpty()) {
            appFilterList = appList
        } else {
            loop@ for (row in appList) {
                val name = formatAppName(row.appName)
                var index = name.indexOf(charSearch)
                if (index == 0) {
                    startResultList.add(row)
                } else if (index > 0) {
                    do {
                        if (name[index - 1] == ' ') {
                            resultList.add(row)
                            continue@loop
                        }
                        index = name.indexOf(charSearch, index + 1)
                    } while (index > 0)
                    endResultList.add(row)
                } else if (name.contains(charSearch)) {
                    endResultList.add(row)
                }
            }
            val offset1 = startResultList.size
            val offset2 = resultList.size
            val length = offset1 + offset2 + endResultList.size
            val list: MutableList<AppInfo> = ArrayList(length)
            for (app in startResultList) {
                list.add(0, app)
            }
            for (app in resultList) {
                list.add(offset1, app)
            }
            for (app in endResultList) {
                list.add(offset1 + offset2, app)
            }
            appFilterList = list
        }
        CoroutineScope(Dispatchers.Main).launch {
            publishResults(appFilterList)
        }
    }

    private fun formatAppName(name: String): String {
        val formatName = name.lowercase(Locale.ROOT)
        var matcher = mMatcher
        if (matcher == null) {
            matcher = Pattern.compile("([^\\p{L}\\d]+)").matcher(formatName)
            mMatcher = matcher
        } else {
            matcher.reset(formatName)
        }
        var stringBuffer = mStringBuffer
        if (stringBuffer == null) {
            stringBuffer = StringBuffer()
            mStringBuffer = stringBuffer
        } else {
            stringBuffer.setLength(0)
        }
        while (matcher!!.find()) {
            matcher.appendReplacement(stringBuffer, " ")
        }
        matcher.appendTail(stringBuffer)
        return stringBuffer.toString().trim()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun publishResults(results: List<AppInfo>?) {
        if (results != null) {
            val length = results.size
            listener.onFoundApp(length > 0)
            if (length >= 0) {
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