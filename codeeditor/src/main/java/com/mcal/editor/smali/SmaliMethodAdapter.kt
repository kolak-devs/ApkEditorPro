package com.mcal.editor.smali

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

class SmaliMethodAdapter(val listener: OnClick, private val methods: List<SmaliMethodInfo>) : RecyclerView.Adapter<SmaliMethodAdapter.SmaliViewHolder>() {
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
        if (text.contains("(")) {
            holder.type.text = "M"
            holder.type.setBackgroundColor(Color.parseColor("#FFAB91"))
            val start = text.indexOf("(")
            val end = text.indexOf(")") + 1
            if (start >= 0 && end >= 0) {
                spanText.setSpan(ForegroundColorSpan(Color.GRAY), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            holder.tv.text = spanText
        } else {
            holder.type.text = "F"
            holder.type.setBackgroundColor(Color.parseColor("#B39DDB"))
            val start = text.indexOf(":")
            val end = text.indexOf(";")
            if (start >= 0 && end >= 0) {
                spanText.setSpan(ForegroundColorSpan(Color.GRAY), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
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