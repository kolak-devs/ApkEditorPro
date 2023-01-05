package com.mcal.apkeditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mcal.apkeditor.activities.ApkInfoActivity;
import com.mcal.apkeditor.dialogs.MatchedLineItem;
import com.mcal.common.utils.ActivityHelper;
import com.mcal.common.view.AutoCompleteAdapter;
import com.mcal.common.view.AutoCompleteTextView;
import com.mcal.editor.TextEditor;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchedTextListAdapter extends BaseExpandableListAdapter implements OnClickListener {
    // File path -> matched lines
    private final Map<String, List<MatchedLineItem>> matchedContents;
    // Do not show path prefix in the group label
    private final String pathPrefix;
    private final String keyword;
    private final ArrayList<String> filePathList;
    private final WeakReference<ApkInfoActivity> activityRef;
    private final WeakReference<ExpandableListView> listviewRef;
    private final boolean[] replaceClicked;
    private final boolean[] editClicked;

    // To decide how many chars to cut
    private int lineTotalWidth = 0;
    private int keywordWidth;

    // For replace function
    private boolean notShowReplaceDlg = false;
    private String strReplace = null;

    public MatchedTextListAdapter(WeakReference<ApkInfoActivity> activityRef, ExpandableListView listView, String pathPrefix, List<String> fileList, String keyword) {
        this.activityRef = activityRef;
        this.listviewRef = new WeakReference<>(listView);
        this.pathPrefix = pathPrefix + "/";
        this.keyword = keyword;

        filePathList = new ArrayList<>();
        matchedContents = new HashMap<>();

        filePathList.addAll(fileList);

        this.replaceClicked = new boolean[fileList.size()];
        this.editClicked = new boolean[fileList.size()];
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        String filePath = filePathList.get(groupPosition);
        List<MatchedLineItem> item = matchedContents.get(filePath);
        if (item != null && childPosition < item.size()) {
            return item.get(childPosition);
        } else {
            return null;
        }
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition * 65536L + childPosition;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        MatchedLineItem matchedLine = (MatchedLineItem) getChild(groupPosition, childPosition);

        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) activityRef.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.item_matchedline, null);
            viewHolder = new ViewHolder();
            viewHolder.matchedLine = (TextView) convertView.findViewById(R.id.tv_line);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String lineRec;
        if (matchedLine != null) {
            Paint paint = new Paint();
            paint.setTextSize(viewHolder.matchedLine.getTextSize());

            if (lineTotalWidth == 0) {
                lineTotalWidth = viewHolder.matchedLine.getWidth();
                keywordWidth = (int) paint.measureText(keyword);
            }

            String header = "" + matchedLine.lineIndex + ": ";
            int headerWidth = (int) paint.measureText(header);
            int requiredWidth = (int) paint.measureText(header + matchedLine.lineContent);

            int cutChars = 0;
            if (lineTotalWidth < requiredWidth) {
                if (matchedLine.matchedPosition > 0) {
                    int actualWidth = (int) paint.measureText(matchedLine.lineContent.substring(0, matchedLine.matchedPosition));
                    int idealWidth = (lineTotalWidth - headerWidth - keywordWidth) / 2;
                    if (actualWidth > idealWidth) {
                        cutChars = matchedLine.matchedPosition - (matchedLine.matchedPosition * idealWidth / actualWidth - 2);
                    }

                    if (cutChars > matchedLine.matchedPosition) {
                        cutChars = matchedLine.matchedPosition;
                    }
                }
            }

            int highlightStart = header.length() + matchedLine.matchedPosition;
            if (cutChars > 0) {
                lineRec = header + "..." + matchedLine.lineContent.substring(cutChars);
                highlightStart -= cutChars - 3;
            } else {
                lineRec = header + matchedLine.lineContent;
            }

            SpannableString sp = new SpannableString(lineRec);
            sp.setSpan(new ForegroundColorSpan(Color.RED), highlightStart, highlightStart + keyword.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            viewHolder.matchedLine.setText(sp);
        }

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        String filePath = filePathList.get(groupPosition);
        List<MatchedLineItem> matched = matchedContents.get(filePath);
        if (matched != null) {
            return matched.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getGroup(int groupPosition) {
        return filePathList.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return filePathList.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String filePath = filePathList.get(groupPosition);
        String groupLabel = filePath.substring(pathPrefix.length());

        GroupViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) activityRef.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.item_matchedfile, null);
            viewHolder = new GroupViewHolder();
            viewHolder.groupLabel = (TextView) convertView.findViewById(R.id.tv_filepath);
            viewHolder.editMenu = convertView.findViewById(R.id.menu_edit);
            viewHolder.replaceMenu = convertView.findViewById(R.id.menu_replace);
            viewHolder.editImage = (ImageView) convertView.findViewById(R.id.image_edit);
            viewHolder.replaceImage = (ImageView) convertView.findViewById(R.id.image_replace);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (GroupViewHolder) convertView.getTag();
        }
        int editResId = editClicked[groupPosition] ? R.drawable.round_edit_blue_24 : (R.drawable.round_edit_24);
        viewHolder.editImage.setImageResource(editResId);

        int replaceId = replaceClicked[groupPosition] ? R.drawable.round_content_copy_blue_24 : (R.drawable.ic_copy);
        viewHolder.replaceImage.setImageResource(replaceId);


        TextView groupTextView = viewHolder.groupLabel;
        groupTextView.setTypeface(null, Typeface.BOLD);
        groupTextView.setText(groupLabel);
        viewHolder.editMenu.setTag(groupPosition);
        viewHolder.editMenu.setOnClickListener(this);
        viewHolder.replaceMenu.setTag(groupPosition);
        viewHolder.replaceMenu.setOnClickListener(this);
        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public ArrayList<String> getFileList() {
        return filePathList;
    }

    public String getKeyword() {
        return keyword;
    }

    // Add the search result so that we can unfold the group
    public void addSearchResult(String filePath, List<MatchedLineItem> matchList) {
        synchronized (matchedContents) {
            matchedContents.put(filePath, matchList);
        }
    }

    public boolean groupChildExist(int groupPosition) {
        String filePath = filePathList.get(groupPosition);
        if (filePath != null) {
            synchronized (matchedContents) {
                return matchedContents.containsKey(filePath);
            }
        }
        return false;
    }

    public void removeSearchResult(int groupPosition) {
        String filePath = filePathList.get(groupPosition);
        if (filePath != null) {
            synchronized (matchedContents) {
                matchedContents.remove(filePath);
            }
        }
    }

    @Override
    public void onClick(@NonNull View v) {
        final ApkInfoActivity activity = activityRef.get();
        int id = v.getId();
        if (id == R.id.menu_edit) {
            Integer index = (Integer) v.getTag();

            if (index < filePathList.size()) {
                if (!editClicked[index]) {
                    editClicked[index] = true;
                    notifyDataSetChanged();
                }

                Intent intent;

                // Allow multiple files editing
                if (filePathList.size() <= 100) {
                    intent = TextEditor.getSoraEditor(activity, filePathList, index, activity.getApkPath(), null, keyword);
                } else {
                    String filePath = filePathList.get(index);
                    intent = TextEditor.getSoraEditor(activity, filePath, activity.getApkPath(), 0, keyword);
                }

                ActivityHelper.attachParam(intent, "searchString", keyword);

                activity.startActivityForResult(intent, 0);
            }
        } else if (id == R.id.menu_replace) {
            Integer index = (Integer) v.getTag();

            if (!replaceClicked[index]) {
                replaceClicked[index] = true;
                notifyDataSetChanged();
            }

            if (index < filePathList.size()) {
                if (notShowReplaceDlg) {
                    replace(index);
                } else {
                    showReplaceDialog(index);
                }
            }
        }
    }

    private void showReplaceDialog(final int index) {
        final ApkInfoActivity activity = activityRef.get();
        final MaterialAlertDialogBuilder materialDialog = new MaterialAlertDialogBuilder(activity);
        materialDialog.setTitle(R.string.replace);
        String msg = String.format(activity.getString(R.string.str_replace_with), keyword);
        materialDialog.setMessage(msg);

        // Set an EditText view to get user input
        final AutoCompleteAdapter adapter = new AutoCompleteAdapter(activity, "search_replace_with");

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        final AutoCompleteTextView input = new AutoCompleteTextView(activity.getApplicationContext());
        input.setAdapter(adapter);
        layout.addView(input, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        final CheckBox notshowCb = new CheckBox(activity);
        notshowCb.setText(R.string.label_replace_with_same_setting);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 20);
        notshowCb.setLayoutParams(params);
        layout.addView(notshowCb);

        materialDialog.setView(layout);

        materialDialog.setPositiveButton(android.R.string.ok,
                (dialog, whichButton) -> {
                    strReplace = input.getText().toString();
                    // Not show the dialog again next time
                    if (notshowCb.isChecked()) {
                        notShowReplaceDlg = true;
                    }
                    // Record the input history
                    if (!"".equals(strReplace.trim())) {
                        adapter.addInputHistory(strReplace);
                    }
                    replace(index);
                });

        materialDialog.setNegativeButton(android.R.string.cancel, null);
        materialDialog.show();
    }

    // Replace the matched string with user input string
    private void replace(int index) {
        final ApkInfoActivity activity = activityRef.get();
        String filePath = filePathList.get(index);
        try {
            replaceWith(filePath, strReplace);
            // Mark the modification
            activity.dealWithModifiedFile(filePath, null);
            // Collapse the group
            listviewRef.get().collapseGroup(index);
            // Remove the child, so next time expand can make it
            // search again
            removeSearchResult(index);
            // Show messages
            String msg = String.format(activity.getString(R.string.str_replaced), keyword);
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void replaceWith(String filePath, String strReplace) throws IOException {
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(filePath, "rw");
            long size = file.length();
            byte[] buffer = new byte[(int) size];
            int offset = 0;
            int read;
            while ((read = file.read(buffer, offset, buffer.length - offset)) > 0) {
                offset += read;
            }
            String content = new String(buffer, StandardCharsets.UTF_8);
            content = content.replace(keyword, strReplace);

            // write
            file.setLength(0);
            file.write(content.getBytes());
        } finally {
            closeQuietly(file);
        }
    }

    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeItem(int position) {
        if (position < filePathList.size()) {
            String path = filePathList.remove(position);
            matchedContents.remove(path);
            notifyDataSetChanged();
        }
    }

    private static class ViewHolder {
        TextView matchedLine;
    }

    private static class GroupViewHolder {
        TextView groupLabel;
        View replaceMenu;
        View editMenu;
        ImageView replaceImage;
        ImageView editImage;
    }
}