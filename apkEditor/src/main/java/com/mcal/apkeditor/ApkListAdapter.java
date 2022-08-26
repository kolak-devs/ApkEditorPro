package com.mcal.apkeditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mcal.common.utils.ApkInfoParser;

import java.util.ArrayList;
import java.util.List;

public class ApkListAdapter extends BaseAdapter {

    private final Context ctx;
    private final LruCache<String, ApkInfoParser.AppInfo> apkInfoCache = new LruCache<String, ApkInfoParser.AppInfo>(
            64) {
//		protected void entryRemoved(boolean evicted, String key,
//				ApkInfoParser.AppInfo oldValue, ApkInfoParser.AppInfo newValue) {
//			Drawable drawable = oldValue.icon;
//			if (drawable instanceof BitmapDrawable) {
//				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
//				Bitmap bitmap = bitmapDrawable.getBitmap();
//				bitmap.recycle();
//			}
//		}
    };
    List<String> fileList = new ArrayList<>();

    public ApkListAdapter(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public int getCount() {
        return fileList.size();
    }

    @Override
    public Object getItem(int position) {
        return fileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String apkFilePath = fileList.get(position);
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(R.layout.item_file, null);

            viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.file_icon);
            viewHolder.filename = (TextView) convertView.findViewById(R.id.filename);
            viewHolder.desc1 = (TextView) convertView.findViewById(R.id.detail1);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Parse the apk icon, label
        ApkInfoParser.AppInfo apkInfo = apkInfoCache.get(apkFilePath);
        if (apkInfo == null) {
            try {
                apkInfo = new ApkInfoParser().parse(ctx, apkFilePath);
            } catch (Throwable ignored) {
            }
            // Cannot parse it
            if (apkInfo == null) {
                apkInfo = new ApkInfoParser.AppInfo();
                apkInfo.icon = ctx.getResources().getDrawable(R.drawable.round_android_24);
            }
        }
        apkInfoCache.put(apkFilePath, apkInfo);

        viewHolder.icon.setImageDrawable(apkInfo.icon);
        if (apkInfo.label != null) {
            viewHolder.filename.setText(apkInfo.label);
            viewHolder.desc1.setText(apkFilePath);
            viewHolder.desc1.setVisibility(View.VISIBLE);
        } else {
            viewHolder.filename.setText(apkFilePath);
            viewHolder.desc1.setVisibility(View.GONE);
        }

        return convertView;
    }

    // Add one apk file to the list
    public void addApkFile(String apkPath) {
        fileList.add(apkPath);
        this.notifyDataSetChanged();
    }

    private static class ViewHolder {
        ImageView icon;
        TextView filename;
        TextView desc1;
    }
}