package com.mcal.apkeditor;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

import com.mcal.apkeditor.activities.ApkComposeActivity;
import com.mcal.apkeditor.editor.TextEditor;
import com.mcal.common.utils.ActivityUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ApkComposeFailAdapter extends BaseAdapter {

    private final WeakReference<ApkComposeActivity> activityRef;
    private final List<String> lines = new ArrayList<>();

    public ApkComposeFailAdapter(ApkComposeActivity activity, String errMessage) {
        this.activityRef = new WeakReference<>(activity);
        updateMessage(errMessage);
    }

    public void updateMessage(String errMessage) {
        lines.clear();

        if (errMessage != null) {
            String[] arr = errMessage.split("\\r?\\n");
            for (String line : arr) {
                // Make sure not empty
                if (!"".equals(line)) {
                    lines.add(line);
                }
            }
        }
    }

    @Override
    public int getCount() {
        return lines.size();
    }

    @Override
    public Object getItem(int position) {
        return lines.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(activityRef.get()).inflate(R.layout.item_failed_message, null);
            holder = new ViewHolder();
            holder.messageTv = convertView.findViewById(R.id.message);
            holder.viewBtn = convertView.findViewById(R.id.btn_view);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Set on click listener
        String strLine = lines.get(position);
        boolean viewable = false;
        int commaPos = strLine.indexOf(':');
        if (commaPos != -1) {
            final String filePath = strLine.substring(0, commaPos);
            if (new File(filePath).exists()) {
                viewable = true;
                holder.viewBtn.setVisibility(View.VISIBLE);

                // Try to get the line number
                int lineNum = -1;
                int nextCommaPos = strLine.indexOf(':', commaPos + 1);
                if (nextCommaPos != -1) {
                    try {
                        lineNum = Integer.parseInt(strLine.substring(commaPos + 1, nextCommaPos));
                    } catch (Exception ignored) {
                    }
                }

                final int lineIndex = lineNum;
                holder.viewBtn.setOnClickListener(v -> {
                    ApkComposeActivity activity = activityRef.get();
                    Intent intent = TextEditor.getSoraEditor(activity, filePath, activity.srcApkPath);
                    if (lineIndex > 0) {
                        ActivityUtils.attachParam(intent, "startLine", "" + lineIndex);
                    }
                    activity.startActivity(intent);
                });
            }
        }
        if (!viewable) { // Cannot view(open the file) for this line
            holder.viewBtn.setVisibility(View.INVISIBLE);
        }

        holder.messageTv.setText(lines.get(position));

        return convertView;
    }

    private static class ViewHolder {
        AppCompatTextView messageTv;
        AppCompatButton viewBtn;
    }
}