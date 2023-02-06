package com.mcal.editor.navigation

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mcal.neweditor.R

class CodeNavigationAdapter(private val listener: CodeNavigationClick, private val methods: List<CodeNavigationInfo>) :
    RecyclerView.Adapter<CodeNavigationAdapter.SmaliViewHolder>() {
    override fun getItemCount(): Int {
        return methods.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmaliViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.popup_item_small, parent, false)
        return SmaliViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SmaliViewHolder, position: Int) {
        val text = methods[position].methodDesc
        val spanText = SpannableString(text)
        val textLength = text.length
        if (text.contains("(")) {
            holder.type.text = "M"
            holder.type.setBackgroundColor(Color.parseColor("#FFAB91"))
            // Красим всё после имени метода
            var start = text.indexOf("(")
            if (start >= 0) {
                spanText.setSpan(ForegroundColorSpan(Color.GRAY), start, textLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            // Красим всё перед именем метода
            do {
                val result = text[start]
                if (result == ' ') {
                    spanText.setSpan(ForegroundColorSpan(Color.GRAY), 0, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    break
                } else {
                    start--
                }
            } while (true)
            holder.tv.text = spanText
        } else {
            holder.type.text = "F"
            holder.type.setBackgroundColor(Color.parseColor("#B39DDB"))
            var start = text.indexOf(":")
            if (start >= 0) {
                // Красим всё после названия поля
                spanText.setSpan(ForegroundColorSpan(Color.GRAY), start, textLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                // Красим всё перед названием поля
                do {
                    val result = text[start]
                    if (result == ' ') {
                        spanText.setSpan(ForegroundColorSpan(Color.GRAY), 0, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        break
                    } else {
                        start--
                    }
                } while (true)
            } else {
                val end = text.lastIndexOf(" ")
                if (end >= 0) {
                    spanText.setSpan(ForegroundColorSpan(Color.GRAY), 0, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            holder.tv.text = spanText
        }
        holder.tv.setOnClickListener {
            listener.onClick(position)
        }
    }

    class SmaliViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv: TextView = itemView.findViewById(R.id.groupItem)
        var type: TextView = itemView.findViewById(R.id.type)
    }
}