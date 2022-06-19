package com.mcal.neweditor.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.mcal.common.AppCommon;
import com.mcal.neweditor.R;
import com.mcal.neweditor.Token;

public class ColorTheme {
    private final int backgroundColor;
    private final int[] foregroundColors = new int[16];

    //
    // theme.1.background = #002b36
    //0 theme.1.normal = #fdf6e3
    //1 theme.1.reserved = #859900
    //2 theme.1.type = #b58900
    //3 theme.1.literal = #2aa198
    //4 theme.1.identifier = #93a1a1
    //5 theme.1.preprocessor = #cb4b16
    //6 theme.1.comment = #586e75
    //7 theme.1.error = #dc322f
    //8 theme.1.function = #268bd2
    //9 theme.1.operator = #93a1a1
    //
    public ColorTheme(Context context) {
        backgroundColor = getColorBackground(context);

        foregroundColors[0] = getColorNormal(context);
        foregroundColors[1] = getColorReserved(context);
        foregroundColors[2] = getColorType(context);
        foregroundColors[3] = getColorLiteral(context);
        foregroundColors[4] = getColorIdentifier(context);
        foregroundColors[5] = getColorPreprocessor(context);
        foregroundColors[6] = getColorComment(context);
        foregroundColors[7] = getColorError(context);
        foregroundColors[8] = getColorFunction(context);
        foregroundColors[9] = getColorOperator(context);
    }

    public int getColorBackground(Context context) {
        return AppCommon.getPreferences().getInt("editorBackground", ContextCompat.getColor(context, R.color.editorBackground));
    }

    public int getColorNormal(Context context) {
        return AppCommon.getPreferences().getInt("editorNormal", ContextCompat.getColor(context, R.color.editorNormal));
    }

    public int getColorReserved(Context context) {
        return AppCommon.getPreferences().getInt("editorReserved", ContextCompat.getColor(context, R.color.editorReserved));
    }

    public int getColorType(Context context) {
        return AppCommon.getPreferences().getInt("editorType", ContextCompat.getColor(context, R.color.editorType));
    }

    public int getColorLiteral(Context context) {
        return AppCommon.getPreferences().getInt("editorLiteral", ContextCompat.getColor(context, R.color.editorLiteral));
    }

    public int getColorIdentifier(Context context) {
        return AppCommon.getPreferences().getInt("editorIdentifier", ContextCompat.getColor(context, R.color.editorIdentifier));
    }

    public int getColorPreprocessor(Context context) {
        return AppCommon.getPreferences().getInt("editorPreprocessor", ContextCompat.getColor(context, R.color.editorPreprocessor));
    }

    public int getColorComment(Context context) {
        return AppCommon.getPreferences().getInt("editorComment", ContextCompat.getColor(context, R.color.editorComment));
    }

    public int getColorError(Context context) {
        return AppCommon.getPreferences().getInt("editorError", ContextCompat.getColor(context, R.color.editorError));
    }

    public int getColorFunction(Context context) {
        return AppCommon.getPreferences().getInt("editorFunction", ContextCompat.getColor(context, R.color.editorFunction));
    }

    public int getColorOperator(Context context) {
        return AppCommon.getPreferences().getInt("editorOperator", ContextCompat.getColor(context, R.color.editorOperator));
    }

    public int getBackgroundColor() {
        return this.backgroundColor;
    }

    public int getForegroundColor(@NonNull Token t) {
        switch (t.id) {
            case Token.COMMENT1:
            case Token.COMMENT2:
            case Token.COMMENT3:
            case Token.COMMENT4:
                return foregroundColors[6];
            case Token.DIGIT:
                return foregroundColors[2];
            case Token.FUNCTION:
                return foregroundColors[8];
            case Token.INVALID:
                return foregroundColors[7];
            case Token.KEYWORD1:
            case Token.KEYWORD2:
            case Token.KEYWORD3:
            case Token.KEYWORD4:
                return foregroundColors[1];
            case Token.LABEL:
                return foregroundColors[4];
            case Token.LITERAL1:
            case Token.LITERAL2:
            case Token.LITERAL3:
            case Token.LITERAL4:
                return foregroundColors[3];
            case Token.MARKUP:
                return foregroundColors[5];
            case Token.OPERATOR:
                return foregroundColors[9];
        }

        return 0xff000000;
    }

    public int getForeground() {
        return foregroundColors[0];
    }
}
