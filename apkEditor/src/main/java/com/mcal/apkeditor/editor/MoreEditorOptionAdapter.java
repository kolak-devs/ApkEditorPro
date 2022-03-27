package com.mcal.apkeditor.editor;

import static com.mcal.apkeditor.editor.TextEditBase.isSmali;
import static com.mcal.apkeditor.editor.TextEditBase.isXml;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.mcal.apkeditor.BuildConfig;
import com.mcal.apkeditor.R;
import com.mcal.common.utils.Pair;

import java.util.ArrayList;
import java.util.List;

// Adapter for the more options
public class MoreEditorOptionAdapter extends BaseAdapter {
    public static final int CMD_DELETE_LINES = 2;
    public static final int CMD_COMMENT_LINES = 5;
    static final int CMD_HTML = 0;
    static final int CMD_COLORPAD = 1;
    static final int CMD_SETTINGS = 3;
    static final int CMD_HELP = 4;
    static final int CMD_CODE_SNIPPET = 6;
    static final int CMD_TO_JAVA = 7;

    private final Context ctx;

    // Commands recorded for all the position
    private final List<Integer> commands = new ArrayList<>();

    // Pair contains image resource id and string id
    private final List<Pair<Integer, Integer>> optionResIds = new ArrayList<>();

    MoreEditorOptionAdapter(Context ctx, String filePath) {
        this.ctx = ctx;

        if (isSmali(filePath) || isXml(filePath)) {
            optionResIds.add(new Pair<>(R.drawable.round_html_24, R.string.html));
            commands.add(CMD_HTML);
        }

        optionResIds.add(new Pair<>(R.drawable.round_palette_24, R.string.colorpad));
        commands.add(CMD_COLORPAD);

        if (isSmali(filePath)) { // Code snippet
            optionResIds.add(new Pair<>(R.drawable.round_content_paste_24, R.string.code_snippet));
            commands.add(CMD_CODE_SNIPPET);
        }

        optionResIds.add(new Pair<>(R.drawable.round_delete_24, R.string.delete_lines));
        commands.add(CMD_DELETE_LINES);

        if (isSmali(filePath)) { // Comment lines & to java code
            optionResIds.add(new Pair<>(R.drawable.round_grid_3x3_24, R.string.comment_lines));
            commands.add(CMD_COMMENT_LINES);

            if (BuildConfig.IS_PRO) {
                optionResIds.add(new Pair<>(R.drawable.round_code_24, R.string.java_code));
                commands.add(CMD_TO_JAVA);
            }
        }

        optionResIds.add(new Pair<>(R.drawable.round_settings_24, R.string.settings));
        commands.add(CMD_SETTINGS);

        optionResIds.add(new Pair<>(R.drawable.round_info_24, R.string.help));
        commands.add(CMD_HELP);
    }

    // Get option number
    public int getOptions() {
        return optionResIds.size();
    }

    public int getCommandByPosition(int position) {
        if (position < commands.size()) {
            return commands.get(position);
        }
        return -1;
    }

    @Override
    public int getCount() {
        return optionResIds.size();
    }

    @Override
    public Object getItem(int position) {
        return optionResIds.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(R.layout.item_more_option, null);
            holder = new ViewHolder();
            holder.image = (AppCompatImageView) convertView.findViewById(R.id.menu_icon);
            holder.title = (AppCompatTextView) convertView.findViewById(R.id.menu_title);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Pair<Integer, Integer> data = optionResIds.get(position);
        if (data.first > 0) {
            holder.image.setImageResource(data.first);
        } else {
            holder.image.setImageBitmap(null);
        }
        holder.title.setText(data.second);

        return convertView;
    }

    private static class ViewHolder {
        public AppCompatImageView image;
        public AppCompatTextView title;
    }
}
