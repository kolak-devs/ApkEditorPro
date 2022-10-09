package com.mcal.editor.smali

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
        holder.tv.text = methods[position].methodDesc
        holder.tv.setOnClickListener {
            listener.onClick(position)
        }
    }

    class SmaliViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv: TextView = itemView.findViewById(R.id.groupItem)
    }
}