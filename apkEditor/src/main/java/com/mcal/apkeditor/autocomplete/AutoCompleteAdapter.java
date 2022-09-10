package com.mcal.apkeditor.autocomplete;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import com.mcal.apkeditor.R;
import com.mcal.common.App;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutoCompleteAdapter extends BaseAdapter implements Filterable {
    private static final int MAX_RECORDS = 32;
    private final Context mContext;
    private final String mTag;
    private final ItemFilter filter;
    public List<String> filteredData;
    private String[] historyWords;

    public AutoCompleteAdapter(Context context, String tag) {
        mContext = context;
        mTag = tag;

        filter = new ItemFilter();

        String history = App.getPreferences().getString(tag, "");
        if (!history.equals("")) {
            historyWords = history.split("\n");
        } else {
            historyWords = new String[0];
        }
        filteredData = new ArrayList<>();
        Collections.addAll(filteredData, historyWords);
    }

    @Override
    public int getCount() {
        return filteredData.size();
    }

    @Override
    public Object getItem(int position) {
        return filteredData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String word = filteredData.get(position);
        AutoCompleteViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.item_autocomplete, null);

            viewHolder = new AutoCompleteViewHolder();
            viewHolder.filename = convertView
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
        return filter;
    }

    public void addInputHistory(String input) {
        ArrayList<String> updatedHistory = new ArrayList<>();
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

        historyWords = updatedHistory.toArray(new String[updatedHistory.size()]);
        App.getPreferences().edit().putString(mTag, sb.toString()).apply();
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
                final ArrayList<String> nlist = new ArrayList<>(count);
                Collections.addAll(nlist, historyWords);
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
