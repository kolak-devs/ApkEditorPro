package com.mcal.apkeditor.dialogs;

public class MatchedLineItem {
    public int lineIndex;
    public int matchedPosition;
    public String lineContent;

    public MatchedLineItem(int lineIndex, int position, String line) {
        this.lineIndex = lineIndex;
        this.matchedPosition = position;
        this.lineContent = line;
    }
}
