package com.mcal.jadx;

import java.io.File;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;

public class Smali2Java {
    public static boolean decompile(String errorMessage, String dexPath, String outputDir) {
        try {
            File dexInputFile = new File(dexPath);
            File javaOutputDir = new File(outputDir);

            JadxArgs args = new JadxArgs();
            args.setOutDirSrc(javaOutputDir);
            args.setSkipResources(true);
            args.setShowInconsistentCode(true);
            args.setInputFile(dexInputFile);

            JadxDecompiler decompiler = new JadxDecompiler(args);
            decompiler.load();
            decompiler.saveSources();

            JavaClass javaClass = decompiler.getClasses().iterator().next();
            javaClass.decompile();
            return true;
        } catch (Exception | StackOverflowError e) {
            errorMessage = "Cannot decompile java code: " + e.getMessage();
        }
        return false;
    }
}
