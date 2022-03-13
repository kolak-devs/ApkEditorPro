package com.mcal.jadx;

import java.io.File;

import jadx.api.JadxDecompiler;

public class Smali2Java {
    public static boolean decompile(String errorMessage, String dexPath, String outputDir) {
        try {
            File dexInputFile = new File(dexPath);
            File javaOutputDir = new File(outputDir);

            JadxDecompiler jadx = new JadxDecompiler();
            jadx.setOutputDir(javaOutputDir);
            jadx.loadFile(dexInputFile);
            jadx.saveSources();

            return true;
        } catch (Exception | StackOverflowError e) {
            errorMessage = "Cannot decompile java code: " + e.getMessage();
        }

        return false;
    }
}