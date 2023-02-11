package com.mcal.downloader

import android.graphics.drawable.Drawable
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DownloaderAdapter(
    private val tools: MutableList<Pair<String, String>?>?,
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
        tools?.get(position)?.let { item ->
            val name = item.first
            val url = item.second
            holder.name.text = name
            holder.url.text = url

            val container = holder.container
            val path = File(ScopedStorage.getBinDir().path + File.separator + name)
            val buttonDownload = holder.download
            val context = buttonDownload.context
            var icon: Drawable?
            val isFileExists = path.exists()
            if (isFileExists) {
                icon = ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.ic_delete,
                    context.theme
                )
                buttonDownload.setImageDrawable(icon)
            } else {
                icon = ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.ic_download,
                    context.theme
                )
                buttonDownload.setImageDrawable(icon)
            }
            buttonDownload.setOnClickListener {
                if (isFileExists) {
                    if (path.delete()) {
                        icon = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.ic_download,
                            context.theme
                        )
                        buttonDownload.setImageDrawable(icon)
                        buttonDownload.isEnabled = true
                    }
                } else {
                    buttonDownload.isEnabled = false
                    CoroutineScope(Dispatchers.IO).launch {
                        NetHelper.download(
                            url,
                            path,
                            container
                        )
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return tools?.size ?: 0
    }

    class AppListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView.findViewById(R.id.container)
        val name: TextView = itemView.findViewById(R.id.name)
        val url: TextView = itemView.findViewById(R.id.url)
        val download: ImageButton = itemView.findViewById(R.id.download)
        val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progressbar)
    }
}