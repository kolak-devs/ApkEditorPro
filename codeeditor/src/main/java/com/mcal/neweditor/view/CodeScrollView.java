package com.mcal.neweditor.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

import com.mcal.neweditor.ScrollViewListener;

public class CodeScrollView extends ScrollView {
    private ScrollViewListener scrollViewListener = null;

    public CodeScrollView(Context context) {
        super(context);
    }

    public CodeScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CodeScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setScrollViewListener(ScrollViewListener scrollViewListener) {
        this.scrollViewListener = scrollViewListener;
    }

    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);
        if (this.scrollViewListener != null) {
            this.scrollViewListener.onScrollChanged(this, x, y, oldx, oldy);
        }
    }
}