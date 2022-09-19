package com.mcal.apkeditor.adapters;

public class LineRecord {
    public String lineData;
    public int lineIndex;
    public int indent;
    // Section start and end line index
    public int sectionStart;
    public int sectionEnd;

    public boolean collapsed = false;
    public boolean deleted = false;
    // Section Tag
    private String tag;

    public LineRecord(int index, String content, String hanging) {
        this.lineIndex = index;
        this.lineData = content;

        // Here, hanging is "\t"
        while (lineData.startsWith(hanging)) {
            this.indent++;
            lineData = lineData.substring(hanging.length());
        }

        while (lineData.startsWith("    ")) {
            this.indent++;
            lineData = lineData.substring(4);
        }

        lineData = lineData.trim();

        sectionStart = -1;
        sectionEnd = -1;
    }

    // Return attr of android:name
    public String getName() {
        String name = null;

        String data = this.lineData;
        int startPos = data.indexOf("android:name=\"");
        if (startPos != -1) {
            startPos += 14;
            int endPos = data.indexOf("\"", startPos);
            if (endPos != -1) {
                name = data.substring(startPos, endPos);
            } else {
                name = data.substring(startPos);
            }
        }
        return name;
    }

    public String getSectionTag() {
        if (this.tag != null) {
            return tag;
        }

        String data = this.lineData;
        int startPos;
        if (this.lineIndex != this.sectionStart) {
            startPos = data.indexOf("</");
            if (startPos != -1) {
                startPos += 2;
            }
        } else {
            startPos = data.indexOf("<");
            if (startPos != -1) {
                startPos += 1;
            }
        }
        if (startPos == -1) {
            return null;
        }

        int endPos = data.indexOf(" ");
        if (endPos == -1) {
            if (this.sectionStart == this.sectionEnd) {
                endPos = data.indexOf("/>");
                if (endPos != -1) {
                    endPos -= 1;
                }
            } else {
                endPos = data.indexOf(">");
            }
        }

        String tag;
        if (endPos != -1) {
            tag = data.substring(startPos, endPos);
        } else {
            tag = data.substring(startPos);
        }
        tag = tag.trim();

        this.tag = tag;
        return tag;
    }
}