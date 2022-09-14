package com.mcal.downloader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.mcal.common.utils.ScopedStorage
import java.io.File

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
        val name = item.first
        val url = item.second
        holder.name.text = name
        holder.url.text = url

        val container = holder.container
        val path = File(ScopedStorage.getBinDir().path + "/" + name)
        val buttonDownload = holder.download
        if (!path.exists()) {
            buttonDownload.setOnClickListener {
                buttonDownload.isEnabled = false
                NetHelper.download(
                    url,
                    path,
                    container
                )
            }
        } else {
            val context = buttonDownload.context
            val icon = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.round_file_download_done,
                context.theme
            )
            buttonDownload.setImageDrawable(icon)
            buttonDownload.isEnabled = false
        }
    }

    override fun getItemCount(): Int {
        return tools.size
    }

    class AppListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView.findViewById(R.id.container)
        val name: TextView = itemView.findViewById(R.id.name)
        val url: TextView = itemView.findViewById(R.id.url)
        val download: ImageButton = itemView.findViewById(R.id.download)
        val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progressbar)
    }
}