package com.common.colormixer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.GlobalConfig;
import com.gmail.heagoo.apkeditor.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ColorValueAdapter extends BaseAdapter {

    private WeakReference<Activity> activityRef;
    private List<ColorValue> values;

    public ColorValueAdapter(Activity activity, List<ColorValue> values) {
        this.activityRef = new WeakReference<>(activity);
        this.values = new ArrayList<>();
        this.values.addAll(values);
    }

    @Override
    public int getCount() {
        return values.size();
    }

    @Override
    public Object getItem(int position) {
        return values.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ColorValue colorVal = values.get(position);

        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(activityRef.get()).inflate(R.layout.item_color_value,
                    null);

            viewHolder = new ViewHolder();
            viewHolder.colorView = (View) convertView
                    .findViewById(R.id.color_view);
            viewHolder.nameTv = (TextView) convertView
                    .findViewById(R.id.tv_name);
            viewHolder.valueTv = (TextView) convertView
                    .findViewById(R.id.tv_value);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        try {
            viewHolder.nameTv.setText(colorVal.name);
            viewHolder.valueTv.setText(colorVal.strColorValue);
            if (colorVal.parsed) {
                viewHolder.colorView.setBackgroundColor(colorVal.intColorValue);
            } else {
                viewHolder.colorView.setBackgroundColor(0xffffffff);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return convertView;
    }

    private static final class ViewHolder {
        View colorView;
        TextView nameTv;
        TextView valueTv;
    }

    public void updateData(ArrayList<ColorValue> colorValues) {
        this.values.clear();
        this.values.addAll(colorValues);
        this.notifyDataSetChanged();
    }
}
