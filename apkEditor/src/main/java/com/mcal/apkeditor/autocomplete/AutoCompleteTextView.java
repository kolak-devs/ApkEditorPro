package com.mcal.apkeditor.autocomplete;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

public class AutoCompleteTextView extends AppCompatAutoCompleteTextView {

    public AutoCompleteTextView(Context context) {
        super(context);
    }

    public AutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }
}