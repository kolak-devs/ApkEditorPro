package com.mcal.apkeditor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

// Used for main menu display
// Projects, Settings, About
public class MenuListAdapter extends BaseAdapter {
    public static final int ITEM_PROJECT = 0;
    public static final int ITEM_FORUM = 1;
    public static final int ITEM_SETTING = 2;
    public static final int ITEM_ABOUT = 3;
    public static final int ITEM_IMG_DOWNLOADER = 4;
    public static final int ITEM_TELEGRAM = 5;
    public static final int ITEM_LOGS = 6;
    // Public version
//    private static final int[] titles = {R.string.projects, R.string.donate, R.string.settings, R.string.about};
//    private static final int[] drawables = {
//            R.drawable.ic_project, R.drawable.ic_donate, R.drawable.ic_setting, R.drawable.ic_about};
//    private static final int[] drawables_dark = {
//            R.drawable.ic_project_white, R.drawable.ic_donate, R.drawable.ic_setting_white, R.drawable.ic_about_white};
//    private static final int[] itemIds = {ITEM_PROJECT, ITEM_DONATE, ITEM_SETTING, ITEM_ABOUT};

    // Google play version
    private static final int[] titles = {R.string.projects, R.string.settings, R.string.image_downloader, R.string.about,
            R.string.view_logs, R.string.link_forum, R.string.link_telegram};
    private static final int[] drawables = {R.drawable.round_inventory_2_24, R.drawable.round_settings_24,
            R.drawable.round_image_24, R.drawable.round_info_24, R.drawable.round_logo_dev_24, R.drawable.outline_link_24,
            R.drawable.outline_link_24};
    private static final int[] itemIds = {ITEM_PROJECT, ITEM_SETTING, ITEM_IMG_DOWNLOADER, ITEM_ABOUT, ITEM_LOGS, ITEM_FORUM, ITEM_TELEGRAM};

    private final WeakReference<Context> ctxRef;

    public MenuListAdapter(Context ctx) {
        this.ctxRef = new WeakReference<>(ctx);
    }

    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public Object getItem(int i) {
        return titles[i];
    }

    @Override
    public long getItemId(int i) {
        return itemIds[i];
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctxRef.get()).inflate(R.layout.item_main_menu, null);

            viewHolder = new ViewHolder();
            viewHolder.iconIv = convertView.findViewById(R.id.menu_icon);
            viewHolder.titleTv = convertView.findViewById(R.id.menu_title);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.iconIv.setImageResource(drawables[i]);
        viewHolder.titleTv.setText(titles[i]);

        return convertView;
    }

    private static class ViewHolder {
        public ImageView iconIv;
        public TextView titleTv;
    }
}
