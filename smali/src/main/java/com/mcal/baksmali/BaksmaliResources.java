package com.mcal.baksmali;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.baksmali.formatter.BaksmaliWriter;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.util.ClassFileNameHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BaksmaliResources {
    public static boolean disassembleDexResource(DexFile dexFile, File outputDir, int jobs, BaksmaliOptions options) {
        return disassembleDexResource(dexFile, outputDir, jobs, options, null);
    }

    public static boolean disassembleDexResource(@NonNull DexFile dexFile, File outputDir, int jobs, final BaksmaliOptions options, @Nullable List<String> classes) {
        ArrayList<ClassDef> arrayList = new ArrayList<>();
        for (ClassDef cls : dexFile.getClasses()) {
            if (cls.getType().contains("/R$")) {
                arrayList.add(cls);
            }
        }
        if (arrayList.isEmpty()) {
            return false;
        }
        List<? extends ClassDef> classDefs = Ordering.natural().sortedCopy(arrayList);
        final ClassFileNameHandler fileNameHandler = new ClassFileNameHandler(outputDir, ".smali");
        ExecutorService executor = Executors.newFixedThreadPool(jobs);
        List<Future<Boolean>> tasks = Lists.newArrayList();
        Set<String> classSet = null;
        if (classes != null) {
            classSet = new HashSet<>(classes);
        }
        for (final ClassDef classDef : classDefs) {
            if (classSet == null || classSet.contains(classDef.getType())) {
                tasks.add(executor.submit(() ->
                        BaksmaliResources.disassembleClass(classDef, fileNameHandler, options)));
            }
        }
        boolean errorOccurred = false;
        try {
            for (Future<Boolean> task : tasks) {
                if (!task.get()) {
                    errorOccurred = true;
                }
            }
            executor.shutdown();
            return !errorOccurred;
        } catch (Throwable th) {
            executor.shutdown();
        }
        return errorOccurred;
    }

    public static boolean disassembleClass(@NonNull ClassDef classDef, ClassFileNameHandler fileNameHandler, BaksmaliOptions options) throws IOException {
        String classDescriptor = classDef.getType();
        if (classDescriptor.charAt(0) == 'L' && classDescriptor.charAt(classDescriptor.length() - 1) == ';') {
            File smaliFile = fileNameHandler.getUniqueFilenameForClass(classDescriptor);
            ClassDefinition classDefinition = new ClassDefinition(options, classDef);
            Writer writer = null;
            try {
                File smaliParent = smaliFile.getParentFile();
                if (!smaliParent.exists() && !smaliParent.mkdirs() && !smaliParent.exists()) {
                    PrintStream printStream = System.err;
                    printStream.println("Unable to create directory " + smaliParent + " - skipping class");
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (Throwable t) {
                            PrintStream printStream2 = System.err;
                            printStream2.println("\n\nError occurred while closing file " + smaliFile.toString());
                            t.printStackTrace();
                        }
                    }
                    return false;
                } else if (smaliFile.exists() || smaliFile.createNewFile()) {
                    BaksmaliWriter writer2 = new BaksmaliWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(smaliFile), StandardCharsets.UTF_8)));
                    classDefinition.writeTo(writer2);
                    if (writer2 != null) {
                        try {
                            writer2.close();
                        } catch (Throwable t) {
                            PrintStream printStream3 = System.err;
                            printStream3.println("\n\nError occurred while closing file " + smaliFile);
                            t.printStackTrace();
                        }
                    }
                    return true;
                } else {
                    PrintStream printStream = System.err;
                    printStream.println("Unable to create file " + smaliFile + " - skipping class");
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (Throwable t) {
                            PrintStream printStream2 = System.err;
                            printStream2.println("\n\nError occurred while closing file " + smaliFile.toString());
                            t.printStackTrace();
                        }
                    }
                    return false;
                }
            } catch (Exception e) {
                try {
                    PrintStream printStream = System.err;
                    printStream.println("\n\nError occurred while disassembling class " + classDescriptor.replace('/', '.') + " - skipping class");
                    e.printStackTrace();
                    smaliFile.delete();
                    if (writer != null) {
                        writer.close();
                    }
                } catch (Throwable t) {
                    PrintStream printStream = System.err;
                    printStream.println("\n\nError occurred while closing file " + smaliFile.toString());
                    t.printStackTrace();
                }
            } catch (Throwable t) {
                PrintStream printStream = System.err;
                printStream.println("\n\nError occurred while closing file " + smaliFile.toString());
                t.printStackTrace();
            }
        } else {
            PrintStream printStream = System.err;
            printStream.println("Unrecognized class descriptor - " + classDescriptor + " - skipping class");
            return false;
        }
        return false;
    }
}