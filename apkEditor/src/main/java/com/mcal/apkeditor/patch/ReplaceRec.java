package com.mcal.apkeditor.patch;

import androidx.annotation.NonNull;

public class ReplaceRec {
    private final int endPos;
    private final String replacing;
    private final int startPos;

    public ReplaceRec(int startPos, int endPos, String replacing) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.replacing = replacing;
    }

    public int getEndPos() {
        return endPos;
    }

    public int getStartPos() {
        return startPos;
    }

    public String getReplacing() {
        return replacing;
    }

    @NonNull
    @Override
    public String toString() {
        return "ReplaceRec{" +
                "endPos=" + endPos +
                ", replacing='" + replacing + '\'' +
                ", startPos=" + startPos +
                '}';
    }
}