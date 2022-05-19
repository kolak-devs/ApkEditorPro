package com.mcal.neweditor.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;

public class CodeHorizontalScrollView extends HorizontalScrollView {
    boolean mFastDirty = true;

    public CodeHorizontalScrollView(Context context) {
        super(context);
    }

    public CodeHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CodeHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void measureChildWithMargins(@NonNull View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        CodeEditor child2 = (CodeEditor) child;
        child2.fastMeasure(getChildMeasureSpec(parentWidthMeasureSpec, (lp.leftMargin + lp.rightMargin) + widthUsed, lp.width), getChildMeasureSpec(parentHeightMeasureSpec, (lp.topMargin + lp.bottomMargin) + heightUsed, lp.height));
    }
}