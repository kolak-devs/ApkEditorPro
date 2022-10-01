package com.mcal.common.utilsOld;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TextFileReader {

    private final List<String> lines = new ArrayList<>();

    public TextFileReader(String filepath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line = br.readLine();
            while (line != null) {
                lines.add(line);
                line = br.readLine();
            }
        }
    }

    public List<String> getLines() {
        return lines;
    }

    public String getContents() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); ++i) {
            sb.append(lines.get(i));
            if (i != lines.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
