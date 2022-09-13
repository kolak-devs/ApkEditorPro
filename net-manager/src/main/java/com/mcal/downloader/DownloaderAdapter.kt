package com.mcal.downloader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DownloaderAdapter(
    private val tools: List<Pair<String, String>>,
) :
    RecyclerView.Adapter<DownloaderAdapter.AppListViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AppListViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_downloader, parent, false)
        return AppListViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
        val item = tools[position]
        holder.name.text = item.first
        holder.url.text = item.second
    }

    override fun getItemCount(): Int {
        return tools.size
    }

    class AppListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.name)
        val url: TextView = itemView.findViewById(R.id.url)
    }
}