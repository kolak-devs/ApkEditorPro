package com.mcal.apkeditor;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.activities.ApkInfoActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MatchedFilenameAdapter extends BaseAdapter implements OnCheckedChangeListener {
    private final WeakReference<ApkInfoActivity> activityRef;
    private final WeakReference<ResSelectionChangeListener> listenerRef;
    private final String pathPrefix;
    private ArrayList<String> matchedFiles;

    private Set<Integer> checkedItems;

    public MatchedFilenameAdapter(ApkInfoActivity activity, ResSelectionChangeListener listener, String pathPrefix, ArrayList<String> matchedFiles) {
        this.activityRef = new WeakReference<>(activity);
        this.listenerRef = new WeakReference<>(listener);
        this.pathPrefix = pathPrefix;
        this.matchedFiles = matchedFiles;

        this.checkedItems = new HashSet<>();
    }

    // Compute how many values are lower than val
    // values in list already sorted
    private static int valuesLowerThan(@NonNull List<Integer> values, int val) {
        int count = 0;
        for (int i = 0; i < values.size(); ++i) {
            int curVal = values.get(i);
            if (curVal < val) {
                count += 1;
            } else {
                break;
            }
        }
        return count;
    }

    @Override
    public int getCount() {
        return matchedFiles.size();
    }

    @Override
    public Object getItem(int position) {
        return matchedFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String filePath = this.matchedFiles.get(position);
        String label = filePath.substring(pathPrefix.length() + 1);

        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(activityRef.get()).inflate(R.layout.item_file_selectable, null);
            viewHolder = new ViewHolder();
            viewHolder.fileIcon = convertView.findViewById(R.id.file_icon);
            viewHolder.pathTv = convertView.findViewById(R.id.filename);
            viewHolder.detailTv = convertView.findViewById(R.id.detail1);
            viewHolder.checkbox = convertView.findViewById(R.id.checkBox);

            viewHolder.pathTv.setSingleLine(false);
            viewHolder.pathTv.setMaxLines(2);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.pathTv.setText(label);
        showDetail(viewHolder, filePath);

        // Checkbox
        viewHolder.checkbox.setId(position);
        viewHolder.checkbox.setChecked(checkedItems.contains(position));
        viewHolder.checkbox.setOnCheckedChangeListener(this);

        return convertView;
    }

    private void showDetail(ViewHolder viewHolder, @NonNull String filePath) {
        // Most general file icon
        int resId = R.drawable.ic_file;

        Bitmap thumbnail = null;
        String detailInfo = null;

        String filename = filePath.substring(filePath.lastIndexOf('/') + 1);
        int dotPos = filename.lastIndexOf('.');
        if (dotPos != -1) {
            String fileType = filename.substring(dotPos + 1);
            if ("xml".equals(fileType)) {
                resId = R.drawable.round_insert_drive_file_yellow_24;
            }
            if ("dex".equals(fileType)) {
                resId = R.drawable.round_insert_drive_file_green_24;
            }
            // Is image file
            else if ("png".equals(fileType) || "jpg".equals(fileType)) {
                ResListAdapter resAdapter = activityRef.get()
                        .getResListAdapter();
                ImageThumbnailInfo info = resAdapter.getImageInfo(filePath);
                thumbnail = info.thumbnail;
                detailInfo = info.detailInfo;
            }
        }

        if (thumbnail != null) {
            viewHolder.fileIcon.setImageBitmap(thumbnail);
        } else {
            viewHolder.fileIcon.setImageResource(resId);
        }

        if (detailInfo != null) {
            viewHolder.detailTv.setVisibility(View.VISIBLE);
            viewHolder.detailTv.setText(detailInfo);
        } else {
            viewHolder.detailTv.setVisibility(View.GONE);
        }
    }

    // Get all selected items, result is sorted
    public List<Integer> getSeletedItems() {
        List<Integer> selected = new ArrayList<Integer>();
        selected.addAll(checkedItems);
        Collections.sort(selected);
        return selected;
    }

    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        int id = view.getId();
        if (isChecked) {
            checkedItems.add(id);
        } else {
            checkedItems.remove(id);
        }

        if (listenerRef != null) {
            listenerRef.get().selectionChanged(checkedItems);
        }
    }

    public void selectAll() {
        for (int i = 0; i < matchedFiles.size(); ++i) {
            checkedItems.add(i);
        }
        notifyDataSetChanged();
    }

    public void selectNone() {
        this.checkedItems.clear();
        this.notifyDataSetChanged();
    }

    // When some items are deleted, this function will be called
    // removedIndexes specifies removed indexes compared to origin files
    public void resetFileList(ArrayList<String> matchedFiles,
                              List<Integer> removedIndexes) {
        Collections.sort(removedIndexes);

        // Compute items that are still checked
        Set<Integer> remainChecked = new HashSet<Integer>();
        for (int item : checkedItems) {
            if (removedIndexes.contains(item)) { // already removed
                continue;
            }
            int count = valuesLowerThan(removedIndexes, item);
            remainChecked.add(item - count);
        }

        this.checkedItems = remainChecked;
        this.matchedFiles = matchedFiles;
        this.notifyDataSetChanged();
    }

    public boolean isAllSelected() {
        return checkedItems.size() == matchedFiles.size();
    }

    public boolean isNonSelected() {
        return checkedItems.isEmpty();
    }

    private static class ViewHolder {
        public ImageView fileIcon;
        public TextView pathTv;
        public TextView detailTv;
        public CheckBox checkbox;
    }
}
