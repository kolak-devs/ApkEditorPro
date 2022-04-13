package com.mcal.apkeditor.autocomplete;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import com.mcal.apkeditor.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoCompleteAdapter extends BaseAdapter implements Filterable {

    private static final int MAX_RECORDS = 32;
    public List<String> filteredData;
    private final Context ctx;
    private final String tag;
    private String[] historyWords;
    private ItemFilter filter;

    public AutoCompleteAdapter(Context ctx, String tag) {
        this.ctx = ctx;
        this.tag = tag;
    }

    private void init() {
        this.filter = new ItemFilter();

        // it list data
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        String history = sp.getString(tag, "");
        // history =
        // "Test\nhello\nandroid:text\nandroid\nhello12\nandroid12\n123\n456\n789\n332";
        if (!history.equals("")) {
            this.historyWords = history.split("\n");
        } else {
            this.historyWords = new String[0];
        }

        this.filteredData = new ArrayList<>();
        this.filteredData.addAll(Arrays.asList(historyWords));
    }

    @Override
    public int getCount() {
        if (filteredData == null) {
            init();
        }
        return filteredData.size();
    }

    @Override
    public Object getItem(int position) {
        if (filteredData == null) {
            init();
        }
        return filteredData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String word = filteredData.get(position);
        AutoCompleteViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(ctx).inflate(
                    R.layout.item_autocomplete, null);

            viewHolder = new AutoCompleteViewHolder();
            viewHolder.filename = (AppCompatTextView) convertView
                    .findViewById(R.id.filename);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (AutoCompleteViewHolder) convertView.getTag();
        }

        viewHolder.filename.setText(word);

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (this.filter == null) {
            init();
        }
        return this.filter;
    }

    public void addInputHistory(String input) {
        if (filteredData == null) {
            init();
        }

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        Editor editor = sp.edit();

        ArrayList<String> updatedHistory = new ArrayList<String>();
        updatedHistory.add(input);
        StringBuilder sb = new StringBuilder();
        sb.append(input);

        for (String word : historyWords) {
            if (!word.equals(input)) {
                updatedHistory.add(word);
                sb.append("\n");
                sb.append(word);
                if (updatedHistory.size() >= MAX_RECORDS) {
                    break;
                }
            }
        }

        this.historyWords = updatedHistory.toArray(new String[updatedHistory
                .size()]);
        editor.putString(this.tag, sb.toString());
        editor.apply();
    }

    static class AutoCompleteViewHolder {
        public AppCompatTextView filename;
    }

    private class ItemFilter extends Filter {
        @NonNull
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint == null) {
                int count = historyWords.length;
                final ArrayList<String> nlist = new ArrayList<String>(count);
                for (String word : historyWords) {
                    nlist.add(word);
                }
                FilterResults results = new FilterResults();
                results.values = nlist;
                results.count = nlist.size();
                return results;
            }

            String filterString = constraint.toString().toLowerCase();

            FilterResults results = new FilterResults();

            int count = historyWords.length;
            final ArrayList<String> nlist = new ArrayList<>(count);

            String filterableString;

            for (int i = 0; i < count; i++) {
                filterableString = historyWords[i];
                if (filterableString.toLowerCase().contains(filterString)) {
                    nlist.add(filterableString);
                }
            }

            results.values = nlist;
            results.count = nlist.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint,
                                      @NonNull FilterResults results) {
            filteredData = (ArrayList<String>) results.values;
            notifyDataSetChanged();
        }

    }
}
