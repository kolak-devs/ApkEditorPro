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
        if (strLine.isEmpty()) {
            return
        }

        var filePath: String? = null
        var lineIndex = 0

        /**
         * Пример ошибки в smali: Source: data/data/com.mcal.apkeditor.pro/files/ApkProtector/smali/a/a.smali;Line: 15;Column: 0
         * Пример ошибки в xml: Source: data/data/com.mcal.apkeditor.pro/files/ApkProtector/AndroidManifest.xml;Line: 15;Message: not well-formed (invalid token)
         */
        val pathMatcher = Pattern.compile("(Source: )(.+)(;)(Line: )(\\d+)(;)((Message: )|(Column: ))(.*)").matcher(strLine)
        if (pathMatcher.find()) {
            filePath = pathMatcher.group(2)?.takeIf { File(it).exists() }?.also {
                /**
                 * Получение пути к файлу с ошибкой
                 */
                holder.pathView.text = buildString {
                    append(activity.getString(R.string.error_path))
                    append(it.replace(ScopedStorage.filesDir.path, ""))
                }
            }
            pathMatcher.group(5)?.let {
                /**
                 * Получение номера строки ошибки
                 */
                holder.lineView.text = buildString {
                    append(activity.getString(R.string.error_line))
                    append(it)
                }
                lineIndex = it.toInt() - 1
            }
            val messageKey = pathMatcher.group(7)
            if (messageKey != null) {
                if (messageKey.startsWith("Message")) {
                    /**
                     * Получение сообщения ошибки - только для xml
                     */
                    pathMatcher.group(10)?.let { value ->
                        holder.messageView.text = buildString {
                            append(activity.getString(R.string.error_message))
                            append(value)
                        }
                    }
                } else if (messageKey.startsWith("Column")) {
                    /**
                     * Получение столбца ошибки - только для smali
                     */
                    pathMatcher.group(10)?.let { value ->
                        holder.messageView.text = buildString {
                            append(activity.getString(R.string.error_column))
                            append(value)
                        }
                    }
                }
            } else {
                /**
                 * Иначе отображаем полную ошибку, как есть
                 */
                holder.messageView.text = buildString {
                    append(activity.getString(R.string.error_message))
                    append(strLine)
                }
            }
        }
        filePath?.let { path ->
            /**
             * Если путь не пустой предлагаем открыть в редакторе
             */
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