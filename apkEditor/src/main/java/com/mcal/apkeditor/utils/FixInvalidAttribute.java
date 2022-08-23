package com.mcal.apkeditor.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mcal.apkeditor.R;
import com.mcal.common.utilsOld.TextFileReader;

import org.jetbrains.annotations.Contract;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FixInvalidAttribute extends FixInvalid {
    private final static String err_InvalidAttr = "^(.+):([0-9]+): Tag (.+) attribute (.+) has invalid character '";
    private final List<InvalidAttrRecord> invalidRecords = new ArrayList<InvalidAttrRecord>();
    // Record all the file modifications
    private final Map<String, Map<String, String>> fileModifications = new HashMap<>();
    private int modifiedFiles = 0;

    // replaces means replaces already made
    public FixInvalidAttribute(String decodeRootPath, String message,
                               Map<String, String> replaces) {
        super(decodeRootPath, message);
        try {
            parseErrorMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isAttrMatched(String name, @NonNull List<String> attrNames) {
        for (String attrName : attrNames) {
            if (name.equals(attrName)) {
                return true;
            }
            if (name.endsWith(":" + attrName)) {
                return true;
            }
        }
        return false;
    }

    // Parse invalid tokens
    private void parseErrorMessage() throws IOException {
        BufferedReader br = null;
        try {
            Pattern pattern = Pattern.compile(err_InvalidAttr);

            br = new BufferedReader(new StringReader(this.errMessage));
            String line = br.readLine();
            while (line != null) {
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    String file = line.substring(m.start(1), m.end(1));
                    String strLine = line.substring(m.start(2), m.end(2));
                    String tagName = line.substring(m.start(3), m.end(3));
                    String attrName = line.substring(m.start(4), m.end(4));

                    int lineNO = -1;
                    try {
                        lineNO = Integer.parseInt(strLine);
                    } catch (Exception e) {
                        continue;
                    }

                    InvalidAttrRecord rec = getInvalidRecord(file);
                    if (rec == null) {
                        rec = new InvalidAttrRecord(decodeRootPath, file,
                                lineNO, tagName, attrName);
                        invalidRecords.add(rec);
                    } else {
                        rec.addErrorInfo(lineNO, tagName, attrName);
                    }
                }
                line = br.readLine();
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }

        // Arrange the record
        for (int i = 0; i < invalidRecords.size(); i++) {
            InvalidAttrRecord rec = invalidRecords.get(i);
            rec.arrange();
        }
    }

    // Get record by file path
    @Nullable
    private InvalidAttrRecord getInvalidRecord(String path) {
        for (int i = 0; i < invalidRecords.size(); i++) {
            InvalidAttrRecord rec = invalidRecords.get(i);
            if (path.equals(rec.filePath)) {
                return rec;
            }
        }
        return null;
    }

    @Override
    public boolean isErrorFixable() {
        return invalidRecords.size() > 0;
    }

    @Override
    public void fixErrors() {
        fixInvalidAttrs();
    }

    private void fixInvalidAttrs() {
        for (int i = 0; i < invalidRecords.size(); i++) {
            InvalidAttrRecord rec = invalidRecords.get(i);
            BufferedWriter bw = null;
            try {
                TextFileReader reader = new TextFileReader(rec.filePath);
                List<String> lines = reader.getLines();

                for (int l = 0; l < lines.size(); l++) {
                    ErrorLine err = rec.getErrorLine(l);
                    if (err == null) {
                        continue;
                    }

                    String newLine = modifyLine(rec.filePath, lines.get(l),
                            err.attrNames);
                    lines.set(l, newLine);
                }

                // Write to file
                bw = new BufferedWriter(new FileWriter(rec.filePath));
                for (String line : lines) {
                    bw.write(line);
                    bw.write("\n");
                }

                this.modifiedFiles += 1;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                ErrorFixManager.closeQuietly(bw);
            }
        }

    }

    @NonNull
    private String modifyLine(String filePath, @NonNull String line,
                              List<String> attrNames) {
        String[] words = line.split(" ");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String[] kv = word.split("=");
            if (kv.length == 2) {
                if (isAttrMatched(kv[0], attrNames)) {
                    String[] replaces = new String[2];
                    words[i] = kv[0] + "=" + makeValidName(kv[1], replaces);
                    if (replaces[1] != null) {
                        addModification(filePath, replaces[0], replaces[1]);
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(word);
            sb.append(" ");
        }

        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private void addModification(String filePath, String originStr,
                                 String newStr) {
        Map<String, String> rec = fileModifications.computeIfAbsent(filePath, k -> new HashMap<>());

        rec.put(originStr, newStr);
    }

    @NonNull
    private String makeValidName(@NonNull String name, String[] replaces) {
        int start = 0;
        int end;
        String pre = "";
        String post = "";

        if (name.startsWith("\"")) {
            start = 1;
            pre = "\"";
        }

        end = name.lastIndexOf("\"");
        if (end == -1) {
            return name;
        }
        post = name.substring(end);

        if (end <= start) {
            return name;
        }

        String originToken = name.substring(start, end);
        String newToken = ErrorFixManager.makeValidToken(originToken);
        replaces[0] = originToken;
        replaces[1] = newToken;

        return pre + newToken + post;
    }

    @Override
    public boolean succeeded() {
        return modifiedFiles > 0;
    }

    @Override
    public String getMofifyMessage(@NonNull Context ctx) {
        return String.format(
                ctx.getString(R.string.str_num_modified_file), modifiedFiles);
    }

    @Override
    public Map<String, Map<String, String>> getAxmlModifications() {
        return fileModifications;
    }

    static class ErrorRecord {
        int lineIndex;
        String tagName;
        String attrName;

        private ErrorRecord(int idx, String tagName, String attrName) {
            this.lineIndex = idx;
            this.tagName = tagName;
            this.attrName = attrName;
        }
    }

    // Arranged in line
    static class ErrorLine {
        int lineIndex;
        List<String> attrNames;

        private ErrorLine(int lineIdx) {
            this.lineIndex = lineIdx;
            attrNames = new ArrayList<>();
        }

        private void addAttrName(String name) {
            attrNames.add(name);
        }
    }

    static class InvalidAttrRecord {
        String filePath;

        // All lines contain invalid attribute
        List<ErrorRecord> errors = new ArrayList<ErrorRecord>();

        List<ErrorLine> errLines = new ArrayList<>();

        private InvalidAttrRecord(String decodedRootPath, @NonNull String filePath,
                                  int lineNO, String tagName, String attrName) {
            if (!filePath.startsWith("/")) {
                filePath = decodedRootPath + filePath;
            }
            this.filePath = filePath;
            int idx = (lineNO > 0 ? (lineNO - 1) : 0);
            ErrorRecord error = new ErrorRecord(idx, tagName, attrName);
            errors.add(error);
        }

        public void addErrorInfo(int lineNO, String tagName, String attrName) {
            int idx = (lineNO > 0 ? (lineNO - 1) : 0);
            ErrorRecord error = new ErrorRecord(idx, tagName, attrName);
            errors.add(error);
        }

        // Arrange all the error record in lines
        public void arrange() {
            for (ErrorRecord err : errors) {
                ErrorLine errLine = getErrorLine(err.lineIndex);
                if (errLine == null) {
                    errLine = new ErrorLine(err.lineIndex);
                    errLines.add(errLine);
                }
                errLine.addAttrName(err.attrName);
            }
        }

        // Get error line record by line index
        @Nullable
        @Contract(pure = true)
        private ErrorLine getErrorLine(int lineIdx) {
            for (ErrorLine line : errLines) {
                if (line.lineIndex == lineIdx) {
                    return line;
                }
            }
            return null;
        }
    }
}
