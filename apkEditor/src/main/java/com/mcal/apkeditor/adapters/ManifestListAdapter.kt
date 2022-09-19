package com.mcal.apkeditor.adapters

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.mcal.apkeditor.R
import com.mcal.apkeditor.dialogs.ManifestLongClickDlg
import com.mcal.apkeditor.dialogs.XmlLineDialog
import com.mcal.apkeditor.dialogs.XmlLineDialog.IXmlLineChanged
import com.mcal.common.utilsOld.BitmapUtils
import com.mcal.patchview.ui.CodeText
import java.io.BufferedReader
import java.io.FileReader
import java.util.*

class ManifestListAdapter(
    activity: Activity,
    manifestPath: String,
    callback: IManifestChangeCallback?
) : RecyclerView.Adapter<ManifestListAdapter.ManifestViewHolder>(), IManifestChangeCallback, IXmlLineChanged {
    private val mManifestLines: MutableList<LineRecord>
    private val mManifestPath: String
    private val mAllXmlLines: MutableList<LineRecord>
    private val mActivity: Activity
    private val mCallback: IManifestChangeCallback?

    init {
        mActivity = activity
        mCallback = callback
        mManifestPath = manifestPath
        mAllXmlLines = ArrayList()
        mManifestLines = ArrayList()
        initData()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManifestViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_manifestline, parent, false)
        return ManifestViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ManifestViewHolder, position: Int) {
        val item = mManifestLines[position]
        holder.lineData.apply {
            text = item.lineData
            setShowLineNumber(false)
        }
        holder.collapseImage.apply {
            if (item.indent > 0) {
                visibility = View.VISIBLE
                setImageBitmap(getImage(item))
                setOnClickListener {
                    synchronized(mManifestLines) {
                        item.collapsed = !item.collapsed
                        updateDisplayLineData()
                    }
                    notifyDataSetChanged()
                }
            } else {
                visibility = View.GONE
            }
        }
        val activity = mActivity
        holder.itemView.setOnClickListener {
            XmlLineDialog(activity, this@ManifestListAdapter, item.lineIndex, item.lineData)
        }
        holder.itemView.setOnLongClickListener {
            ManifestLongClickDlg(activity, mManifestPath, item, this@ManifestListAdapter)
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int {
        return mManifestLines.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    /**
     * Инициализация данных
     */
    private fun initData() {
        try {
            val br = BufferedReader(FileReader(mManifestPath))
            var index = 0
            var data: String?
            while (br.readLine().also { data = it } != null) {
                val rec = LineRecord(index++, data, "\t")
                mAllXmlLines.add(rec)
                mManifestLines.add(rec)
            }
            br.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        initXmlLines()
    }

    private fun initXmlLines() {
        try {
            val stack = Stack<LineRecord>()
            for (i in mManifestLines.indices) {
                val lines = mManifestLines[i]
                if (lines.indent <= 0) {
                    continue
                }
                // Конец блока
                if (lines.lineData.endsWith("/>")) {
                    lines.sectionStart = lines.lineIndex
                    lines.sectionEnd = lines.lineIndex
                } else if (lines.lineData.startsWith("</")) {
                    val startLine = stack.pop()
                    startLine.sectionEnd = lines.lineIndex
                    lines.sectionStart = startLine.lineIndex
                    lines.sectionEnd = lines.lineIndex
                } else {
                    lines.sectionStart = lines.lineIndex
                    stack.push(lines)
                }
            }
        } catch (e: EmptyStackException) {
            var i = 0
            while (i < mManifestLines.size) {
                val lr = mManifestLines[i]
                lr.indent = 0
                i++
            }
        }
    }

    /**
     * Обновляет данные для свернутого отображения
     * Пропуск строк и удаление элементов
     */
    private fun updateDisplayLineData() {
        mManifestLines.clear()
        var i = 0
        while (i < mAllXmlLines.size) {
            val rec = mAllXmlLines[i]
            if (rec.deleted) {
                i++
                continue
            }
            mManifestLines.add(rec)

            // Пропускаем несколько строк
            if (rec.collapsed) {
                if (rec.sectionEnd > i) {
                    i = rec.sectionEnd
                }
            }
            i++
        }
    }

    private fun getImage(lineRec: LineRecord): Bitmap {
        val indent = lineRec.indent
        val height = 48
        val width = 48 * indent
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        if (lineRec.sectionEnd != lineRec.lineIndex) {
            val arrow: Bitmap = if (!lineRec.collapsed) {
                BitmapUtils.getBitmapFromVectorDrawable(mActivity, R.drawable.manifest_chevron_down)
            } else {
                BitmapUtils.getBitmapFromVectorDrawable(mActivity, R.drawable.manifest_chevron_right)
            }

            // Draw the arrow
            val canvas = Canvas(bitmap)
            val paint = Paint()
            canvas.drawBitmap(arrow, (width - 48).toFloat(), 0f, paint)
        }
        return bitmap
    }

    override fun tryToDeleteSection(lineRec: LineRecord): String? {
        if (!isSectionDeletable(lineRec)) {
            return mActivity.resources.getString(R.string.section_undeletable)
        }

        // To hide some lines
        val startLine = lineRec.sectionStart
        val endLine = lineRec.sectionEnd
        val contentBuffer = StringBuilder()
        synchronized(mManifestLines) {
            for (i in mAllXmlLines.indices) {
                val rec = mAllXmlLines[i]
                if (rec.lineIndex in startLine..endLine) {
                    rec.deleted = true
                }
                if (!rec.deleted) {
                    contentBuffer.append(rec.lineData)
                    contentBuffer.append('\n')
                }
            }
            updateDisplayLineData()
        }
        notifyDataSetChanged()

        // Further callback to save the content
        mCallback?.manifestChanged(contentBuffer.toString())
        return null
    }

    /**
     * Проверяет какие блоки можно удалить
     */
    private fun isSectionDeletable(lineRec: LineRecord): Boolean {
        when (lineRec.sectionTag) {
            "manifest", "application" -> {
                return false
            }
            "activity", "intent-filter" -> {
                return !containMainAction(lineRec)
            }
            "action" -> {
                return !lineRec.lineData.contains("android.intent.action.MAIN")
            }
            "category" -> {
                val bContain = lineRec.lineData.contains("android.intent.category.LAUNCHER")
                return !bContain
            }
            else -> return true
        }
    }

    /**
     * Если это главная Activity, запретить возможность удаления
     */
    private fun containMainAction(lineRec: LineRecord): Boolean {
        for (i in lineRec.sectionStart until lineRec.sectionEnd) {
            val rec = mAllXmlLines[i]
            // This is the main activity
            if ("action" == rec.sectionTag && rec.lineData.contains("android.intent.action.MAIN")) {
                return true
            }
        }
        return false
    }

    override fun xmlLineChanged(lineIndex: Int, newLine: String) {
        val contentBuffer = StringBuilder()
        synchronized(this) {
            for (rec in mAllXmlLines) {
                if (rec.lineIndex == lineIndex) {
                    rec.lineData = newLine
                }
                if (!rec.deleted) {
                    contentBuffer.append(rec.lineData)
                    contentBuffer.append('\n')
                }
            }
            for (rec in mManifestLines) {
                if (rec.lineIndex == lineIndex) {
                    rec.lineData = newLine
                    break
                }
            }
        }
        notifyDataSetChanged()

        manifestChanged(contentBuffer.toString())
    }

    /**
     * Сообщаем слушателю, что манифест был изменён.
     * Нужно обновить файл
     */
    override fun manifestChanged(newContent: String) {
        mCallback?.manifestChanged(newContent)
    }

    fun reload() {
        synchronized(mManifestLines) {
            mAllXmlLines.clear()
            mManifestLines.clear()
            initData()
        }
        notifyDataSetChanged()
    }

    class ManifestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val collapseImage: ImageView = itemView.findViewById(R.id.collapse_icon)
        val lineData: CodeText = itemView.findViewById(R.id.line_data)
    }
}