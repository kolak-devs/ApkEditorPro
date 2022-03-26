package com.mcal.neweditor;

import java.io.Serializable;

import android.text.TextPaint;
import android.text.style.CharacterStyle;

public class SyntaxHighlightSpan extends CharacterStyle implements Serializable {
    private static final long serialVersionUID = 218554000863866749L;
    public int mColor;

    public SyntaxHighlightSpan(int color) {
        this.mColor = color;
    }

    public void updateDrawState(TextPaint textpaint) {
        textpaint.setColor(this.mColor);
    }
}