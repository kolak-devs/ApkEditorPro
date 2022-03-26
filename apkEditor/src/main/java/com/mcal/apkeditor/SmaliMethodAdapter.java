package com.mcal.apkeditor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.appcompat.widget.AppCompatTextView;

import com.mcal.apkeditor.R;

import java.util.List;

/**
 * Created by phe3 on 1/31/2017.
 */

public class SmaliMethodAdapter extends BaseAdapter {
    private final Context ctx;
    private final List<SmaliMethodInfo> methods;

    public SmaliMethodAdapter(Context ctx, List<SmaliMethodInfo> methods) {
        this.ctx = ctx;
        this.methods = methods;
    }

    @Override
    public int getCount() {
        return methods.size();
    }

    @Override
    public Object getItem(int position) {
        return methods.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(R.layout.popup_item_small, null);
            viewHolder = new ViewHolder();
            viewHolder.tv = convertView.findViewById(R.id.groupItem);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Set the method description
        viewHolder.tv.setText(methods.get(position).methodDesc);

        return convertView;
    }

    static class ViewHolder {
        public AppCompatTextView tv;
    }
}
