package com.mcal.permissioneditor.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mcal.permissioneditor.model.Permission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManifestAdapter extends BaseAdapter {
    public Context mContext;
    private List<Permission> mCurrentList;
    private Listener mListener;
    private boolean mSelectMode;
    private Map<String, Boolean> mSelected = new HashMap<>();

    public ManifestAdapter(Context context, List<Permission> list) {
        this.mContext = context;
        this.mCurrentList = list;
    }

    public static void remove(@NonNull List<Permission> list, Permission permission) {
        for (int size = list.size() - 1; size >= 0; size--) {
            if (list.get(size).getName().equals(permission.getName())) {
                list.remove(size);
            }
        }
    }

    public static void remove(@NonNull List<Permission> list, String str) {
        for (int size = list.size() - 1; size >= 0; size--) {
            if (list.get(size).getName().equals(str)) {
                list.remove(size);
            }
        }
    }

    public static boolean hasAdded(@NonNull List<Permission> list, String str) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(str)) {
                return true;
            }
        }
        return false;
    }

    public static int dp2px(float f) {
        return (int) ((Resources.getSystem().getDisplayMetrics().density * f) + 0.5f);
    }

    public void setListener(Listener mListener) {
        this.mListener = mListener;
    }

    public boolean isSelectMode() {
        return mSelectMode;
    }

    public void setSelectMode(boolean z) {
        if (z) {
            if (isSelectMode()) {
                return;
            }
        } else if (!isSelectMode()) {
            return;
        }
        mSelectMode = z;
        getSelected().clear();
    }

    public void setSelected(Permission permission, boolean z) {
        if (z) {
            getSelected().put(permission.getName(), new Boolean(z));
        } else {
            getSelected().remove(permission.getName());
        }
        if (mListener != null) {
            mListener.onSelection(permission, z);
        }
    }

    public Map<String, Boolean> getSelected() {
        return mSelected;
    }

    public void setSelected(Map<String, Boolean> map) {
        mSelected = map;
    }

    @Override
    public void notifyDataSetChanged() {
        if (mListener != null) {
            mListener.onDataSetChanged();
        }
        if (getCurrentList().isEmpty()) {
            notifyDataSetInvalidated();
        } else {
            super.notifyDataSetChanged();
        }
    }

    @Override
    public void notifyDataSetInvalidated() {
        super.notifyDataSetInvalidated();
    }

    public boolean isSelected(String str) {
        if (mSelected.containsKey(str)) {
            return mSelected.get(str);
        }
        return false;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View itemView;
        if (view == null) {
            itemView = new ItemView(mContext);
        } else {
            itemView = view;
        }
        ViewHolder viewHolder = ((ItemView) itemView).holder;
        Permission item = getItem(i);
        viewHolder.title.setText(new SpannableStringBuilder().append(String.format("%d. ", i + 1)).append(getItemTitle(i)));
        viewHolder.subtitle.setText(getItemSubTitle(i));
        viewHolder.setSelection(isSelected(item.getName()));
        viewHolder.setSelectionMode(isSelectMode());
        return itemView;
    }

    public CharSequence getItemTitle(int i) {
        return getItem(i).getLabel();
    }

    public CharSequence getItemSubTitle(int i) {
        return getItem(i).getName();
    }

    public List<Permission> getCurrentList() {
        return mCurrentList;
    }

    public void setCurrentList(List<Permission> list) {
        mCurrentList = list;
    }

    @Override
    public int getCount() {
        return getCurrentList().size();
    }

    @Override
    public Permission getItem(int i) {
        return getCurrentList().get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public interface Listener {
        void onDataSetChanged();

        void onSelection(Permission permission, boolean z);
    }

    public static class ItemView extends LinearLayout {
        public ViewHolder holder = new ViewHolder();

        @SuppressLint("ResourceType")
        public ItemView(Context context) {
            super(context);
            CheckBox checkBox = new CheckBox(context);
            checkBox.setClickable(false);
            checkBox.setFocusable(false);
            checkBox.setFocusableInTouchMode(false);

            TextView title = new TextView(context);
            title.setTextAppearance(context, 16973890);
            title.setTextSize((float) 15);

            TextView subtitle = new TextView(context);
            subtitle.setTextAppearance(context, 16973894);
            subtitle.setTextSize((float) 14);

            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.addView(checkBox);
            linearLayout.setPadding(0, 0, ManifestAdapter.dp2px((float) 16), 0);

            LinearLayout linearLayout2 = new LinearLayout(context);
            linearLayout2.setOrientation(1);
            linearLayout2.addView(title);
            linearLayout2.addView(subtitle);

            setGravity(16);
            setPadding(ManifestAdapter.dp2px((float) 16), ManifestAdapter.dp2px((float) 13), ManifestAdapter.dp2px((float) 16), ManifestAdapter.dp2px((float) 13));
            addView(linearLayout);
            addView(linearLayout2);
            holder.cb = checkBox;
            holder.title = title;
            holder.subtitle = subtitle;
        }
    }

    public static class ViewHolder {
        public TextView subtitle;
        public TextView title;
        private CheckBox cb;

        public void setSelection(boolean z) {
            cb.setChecked(z);
        }

        public boolean invertSelection() {
            setSelection(!cb.isChecked());
            return cb.isChecked();
        }

        @SuppressLint("WrongConstant")
        public void setSelectionMode(boolean z) {
            ((View) cb.getParent()).setVisibility(z ? 0 : 8);
        }
    }
}