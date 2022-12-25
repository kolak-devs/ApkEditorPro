package com.mcal.apkeditor.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mcal.apkeditor.R
import com.mcal.apkeditor.activities.ApkComposeActivity
import com.mcal.common.utils.ScopedStorage
import com.mcal.editor.TextEditor.getSoraEditor
import java.io.File
import java.util.regex.Pattern

class ApkComposeFailAdapter(
    private val activity: ApkComposeActivity, errMessage: String?
) : RecyclerView.Adapter<ApkComposeFailAdapter.ApkComposeFailViewHolder>() {
    private val lines: MutableList<String> = ArrayList()

    init {
        updateMessage(errMessage)
    }

    private fun updateMessage(errMessage: String?) {
        lines.clear()
        if (errMessage != null) {
            lines.add(errMessage)
        }
    }

    override fun getItemCount(): Int {
        return lines.size
    }

    fun getItem(position: Int): Any {
        return lines[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApkComposeFailViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_failed_message, parent, false)
        return ApkComposeFailViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ApkComposeFailViewHolder, position: Int) {
        val strLine = lines[position]

        var filePath: String? = null
        var lineIndex = 0

        val pathMatcher = Pattern.compile("(Source: )(.+)(;)(Line: )(\\d+)(;)(Message: )(.*)").matcher(strLine)
        if (pathMatcher.find()) {
            filePath = pathMatcher.group(2)?.takeIf { File(it).exists() }?.also {
                holder.pathView.text = "Path: ${it.replace(ScopedStorage.filesDir.path, "")}"
            }
            pathMatcher.group(5)?.let {
                holder.lineView.text = "Line: $it"
                lineIndex = it.toInt()
            }
            pathMatcher.group(7)?.let { key ->
                if (key.startsWith("Message")) {
                    pathMatcher.group(8)?.let { value ->
                        holder.messageView.text = "Message: $value"
                    }
                } else if (key.startsWith("Column")) {
                    pathMatcher.group(8)?.let { value ->
                        holder.messageView.text = "Column: $value"
                    }
                } else {

                }
            } ?: run {
                holder.messageView.text = "Message: $strLine"
            }
        }
        filePath?.let { path ->
            holder.editor.setOnClickListener {
                val intent = getSoraEditor(activity, path, activity.srcApkPath, lineIndex, null)
                activity.startActivity(intent)
            }
        } ?: run {
            holder.editor.visibility = View.GONE
        }
    }

    class ApkComposeFailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pathView: TextView = itemView.findViewById(R.id.path)
        val lineView: TextView = itemView.findViewById(R.id.line)
        val messageView: TextView = itemView.findViewById(R.id.message)
        val editor: Button = itemView.findViewById(R.id.editor)
    }
}