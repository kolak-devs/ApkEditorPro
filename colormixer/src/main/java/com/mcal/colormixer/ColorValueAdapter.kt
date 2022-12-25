package com.mcal.apkeditor.colormixer

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.mcal.colormixer.R
import java.lang.ref.WeakReference

class ColorValueAdapter(
    activity: Activity,
    values: List<ColorValue>?
) : BaseAdapter() {
    private val weakReference: WeakReference<Activity>
    private val mValues: MutableList<ColorValue>

    init {
        weakReference = WeakReference(activity)
        mValues = ArrayList()
        mValues.addAll(values!!)
    }

    override fun getCount(): Int {
        return mValues.size
    }

    override fun getItem(position: Int): Any {
        return mValues[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val colorVal = mValues[position]
        val viewHolder: ViewHolder?
        if (view == null) {
            view = LayoutInflater.from(weakReference.get()).inflate(
                R.layout.item_color_value,
                null
            )
            viewHolder = ViewHolder()
            viewHolder.colorView = view.findViewById(R.id.color_view) as View
            viewHolder.nameTv = view.findViewById<View>(R.id.tv_name) as TextView
            viewHolder.valueTv = view.findViewById<View>(R.id.tv_value) as TextView
            view!!.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }
        try {
            viewHolder.nameTv?.text = colorVal.name
            viewHolder.valueTv?.text = colorVal.strColorValue
            if (colorVal.parsed) {
                viewHolder.colorView?.setBackgroundColor(colorVal.intColorValue)
            } else {
                viewHolder.colorView?.setBackgroundColor(-0x1)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return view
    }

    fun updateData(colorValues: ArrayList<ColorValue>?) {
        mValues.clear()
        colorValues?.let { list ->
            mValues.addAll(list)
        }
        notifyDataSetChanged()
    }

    private class ViewHolder {
        var colorView: View? = null
        var nameTv: TextView? = null
        var valueTv: TextView? = null
    }
}