package com.mcal.apkeditor.pro;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.BuildConfig;
import com.mcal.apkeditor.inf.IJavaExtractor;
import com.mcal.common.utils.IOUtils;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.immutable.ImmutableDexFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.plugins.input.dex.DexInputPlugin;

public class JavaExtractor implements IJavaExtractor {

    private final String apkPath;
    private final String dexName;
    private final String className;
    private String interestedName;
    private final File workingDirectory;

    private String errorMessage = null;

    public JavaExtractor(String apkPath, String dexName, @NonNull String className, String workingDirectory) {
        this.apkPath = apkPath;
        this.dexName = dexName;
        this.className = className;
        this.workingDirectory = new File(workingDirectory);

        this.interestedName = className;
        int position = className.lastIndexOf('$');
        if (position != -1) {
            interestedName = className.substring(0, position);
        }
    }

    public boolean decompile(File code, File targetFilePath) {
        try {
            JadxArgs args = new JadxArgs();
            args.setSkipResources(true);
            args.setShowInconsistentCode(true);
            args.setInputFile(code);
            args.setOutDirSrc(targetFilePath);

            JadxDecompiler decompiler = new JadxDecompiler(args);
            decompiler.load();
            decompiler.saveSources();

            writeDexFile(code, targetFilePath);
            return true;
        } catch (Exception|StackOverflowError e) {
            e.printStackTrace();
            errorMessage="Cannot decompile java code: " + e.getMessage();
            return false;
        }
    }

    public void writeDexFile(File dex, File targetFilePath) throws IOException {
        try (JadxDecompiler jadx = new JadxDecompiler();
             InputStream in = new FileInputStream(dex)) {
            jadx.addCustomLoad(new DexInputPlugin().loadDexFromInputStream(in, workingDirectory + "/extracted.dex"));
            jadx.load();
            for (JavaClass cls : jadx.getClasses()) {
                File path =new File(targetFilePath + File.separator + cls.getPackage().replace(".", "/"));
                if(!path.exists()) {
                    path.mkdirs();
                }
                IOUtils.writeToFile(cls.getCode(), path + File.separator + cls.getName() + ".java");
            }
        }
    }

    @Override
    public boolean extract() {
        if (extractDex()) {
            File dexPath = new File(workingDirectory + "/extracted.dex");
            return decompile(dexPath, workingDirectory);
        }
        return false;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    private boolean extractDex() {
        // Load dex file
        DexFile dexFile = null;
        try {
            dexFile = DexFileFactory.loadDexEntry(new File(apkPath), dexName, true, Opcodes.forApi(15)).getDexFile();
        } catch (Exception e) {
            errorMessage = "The dex file cannot be decompiled.";
            return false;
        }

        // Filter classes
        List<ClassDef> classes = new ArrayList<>();
        for (ClassDef classDef : dexFile.getClasses()) {
            final String currentClass = classDef.getType();
            if (currentClass.startsWith(interestedName)) {
                classes.add(classDef);
            }
        }

        // Check directory
        File dir = workingDirectory;
        if (!dir.exists()) {
            dir.mkdirs();
        }

        dexFile = new ImmutableDexFile(Opcodes.forApi(15), classes);

        try {
            DexFileFactory.writeDexFile(workingDirectory + "/extracted.dex", dexFile);
        } catch (Exception e) {
            errorMessage = "Cannot extract " + className
                    + " as dex extract failed: " + e.getMessage();
            return false;
        }

        return true;
    }
}
