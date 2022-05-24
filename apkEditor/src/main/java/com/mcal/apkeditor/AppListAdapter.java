package com.mcal.apkeditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AppListAdapter extends BaseAdapter {

    private final Context ctx;
    private final PackageManager pm;
    private final LruCache<String, Drawable> pkgDrawables = new LruCache<String, Drawable>(
            32) {
        protected void entryRemoved(boolean evicted, String key,
                                    @NonNull Drawable oldValue, Drawable newValue) {
            oldValue.setCallback(null);
        }
    };
    public List<AppInfo> appList = new ArrayList<AppInfo>();
    private APPLIST_ORDER order;

    public AppListAdapter(@NonNull Context ctx) {
        this.ctx = ctx;
        this.pm = ctx.getPackageManager();
    }

    public void setAppList(List<AppInfo> appList, @NonNull String order) {
        String[] orderConsts = ctx.getResources().getStringArray(
                R.array.order_value);
        if (order.equals(orderConsts[0])) {
            this.order = APPLIST_ORDER.BY_NAME;
        } else if (order.equals(orderConsts[1])) {
            this.order = APPLIST_ORDER.BY_INSTALL_TIME;
        } else {
            this.order = APPLIST_ORDER.BY_NAME;
        }
        sortAppList(appList);
        synchronized (this.appList) {
            this.appList.clear();
            this.appList.addAll(appList);
        }
    }

    private void sortAppList(List<AppInfo> appList) {
        final Locale locale = Locale.getDefault();
        Comparator<AppInfo> comparator = null;

        switch (order) {
            case BY_NAME:
                comparator = Comparator.comparing(arg0 -> arg0.appName.toLowerCase(locale));
                break;
            case BY_INSTALL_TIME:
                comparator = (arg0, arg1) -> arg0.lastUpdateTime < arg1.lastUpdateTime ? 1 : -1;
                break;
        }

        appList.sort(comparator);
    }

    public List<AppInfo> getAppList() {
        synchronized (this.appList) {
            List<AppInfo> retList = new ArrayList<>(appList);
            return retList;
        }
    }

    @Override
    public int getCount() {
        synchronized (this.appList) {
            return this.appList.size();
        }
    }

    @Override
    public Object getItem(int arg0) {
        synchronized (this.appList) {
            return this.appList.get(arg0);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final AppInfo appInfo = (AppInfo) getItem(position);
        if (appInfo == null) {
            return null;
        }

        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(R.layout.item_applist, null);

            viewHolder = new ViewHolder();
            viewHolder.icon = convertView
                    .findViewById(R.id.app_icon);
            viewHolder.appName = convertView
                    .findViewById(R.id.app_name);
            viewHolder.desc1 = convertView
                    .findViewById(R.id.app_desc1);
            viewHolder.desc2 = convertView
                    .findViewById(R.id.app_desc2);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        try {
            viewHolder.appName.setText(appInfo.appName);

            // if (appInfo.isSysApp)
            // viewHolder.appName.setTextColor(0xfff75343);
            // else
            // viewHolder.appName.setTextColor(0xff0028c6);

            viewHolder.desc1.setText(appInfo.packagePath);

            Drawable icon = pkgDrawables.get(appInfo.packagePath);
            if (icon == null) {
                icon = appInfo.applicationInfo.loadIcon(pm);
                pkgDrawables.put(appInfo.packagePath, icon);
            }

            viewHolder.icon.setImageDrawable(icon);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // appCustomize.setupLongClickListener(convertView, appInfo);

        // appCustomize.setupClickListener(convertView, appInfo);

        return convertView;
    }

    private enum APPLIST_ORDER {
        BY_NAME, BY_INSTALL_TIME
    }

    static class ViewHolder {
        public ImageView icon;
        public TextView desc2;
        public TextView desc1;
        public TextView appName;
    }
}