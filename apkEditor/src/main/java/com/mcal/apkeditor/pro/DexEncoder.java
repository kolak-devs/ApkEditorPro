package com.mcal.apkeditor.pro;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.writer.builder.DexBuilder;
import org.jf.dexlib2.writer.io.FileDataStore;
import org.jf.smali.SmaliOptions;
import org.jf.smali.smaliFlexLexer;
import org.jf.smali.smaliParser;
import org.jf.smali.smaliTreeWalker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DexEncoder {
    public static void smali2Dex(String srcDirectory, String outputDexFile) throws Exception {
        final SmaliOptions options = new SmaliOptions();

        options.jobs = Runtime.getRuntime().availableProcessors();
        options.apiLevel = 15;
        options.outputDexFile = outputDexFile;
        options.allowOdexOpcodes = false;
        options.verboseErrors = false;

        long startTime = System.currentTimeMillis();
        try {
            LinkedHashSet<File> filesToProcessSet = new LinkedHashSet<>();

            getSmaliFilesInDir(new File(srcDirectory), filesToProcessSet);

            //Log.d("DEBUG", "jobs=" + jobs);

            boolean errors = false;

            final DexBuilder dexBuilder = new DexBuilder(Opcodes.forApi(15));

            ExecutorService executor = Executors.newFixedThreadPool(options.jobs);
            List<Future<Boolean>> tasks = Lists.newArrayList();

            for (final File file : filesToProcessSet) {
                tasks.add(executor.submit(() -> assembleSmaliFile(file, dexBuilder, options)));
            }

            for (Future<Boolean> task : tasks) {
                while (true) {
                    try {
                        if (!task.get()) {
                            errors = true;
                        }
                    } catch (InterruptedException ex) {
                        continue;
                    }
                    break;
                }
            }

            executor.shutdown();

            if (errors) {
                throw new Exception("Encountered errors while compiling smali files.");
            }

            dexBuilder.writeTo(new FileDataStore(new File(options.outputDexFile)));

        } catch (RuntimeException ex) {
            ex.printStackTrace();
            throw new Exception("UNEXPECTED TOP-LEVEL EXCEPTION: " + ex.getMessage());
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw new Exception("UNEXPECTED TOP-LEVEL ERROR: " + ex.getMessage());
        }

        long endTime = System.currentTimeMillis();
        //Log.d("DEBUG", "time="+ (endTime - startTime));
    }

    private static void getSmaliFilesInDir(@NonNull File dir, Set<File> smaliFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    getSmaliFilesInDir(file, smaliFiles);
                } else if (file.getName().endsWith(".smali")) {
                    smaliFiles.add(file);
                }
            }
        }
    }

    private static boolean assembleSmaliFile(File smaliFile, DexBuilder dexBuilder, @NonNull SmaliOptions options)
            throws Exception {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(smaliFile);
            InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8);

            smaliFlexLexer lexer = new smaliFlexLexer(reader, options.apiLevel);
            lexer.setSourceFile(smaliFile);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

//            if (options.printTokens) {
//                tokens.getTokens();
//
//                for (int i=0; i<tokens.size(); i++) {
//                    Token token = tokens.get(i);
//                    if (token.getChannel() == smaliParser.HIDDEN) {
//                        continue;
//                    }
//
//                    System.out.println(smaliParser.tokenNames[token.getType()] + ": " + token.getText());
//                }
//
//                System.out.flush();
//            }

            smaliParser parser = new smaliParser(tokens);
            parser.setVerboseErrors(options.verboseErrors);
            parser.setAllowOdex(options.allowOdexOpcodes);
            parser.setApiLevel(options.apiLevel);

            smaliParser.smali_file_return result = parser.smali_file();

            if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
                String sourceName = lexer.getSourceName();
                String errorMsg = "Source: " + sourceName + " Line: " + lexer.getLine() +
                        " Column: " + lexer.getColumn();//.getErrorMessages();
                if (sourceName.equals("")) {
                    throw new Exception("Error occurred while compiling " + smaliFile.getName());
                } else {
                    throw new Exception(errorMsg);
                }
            }

            CommonTree t = result.getTree();

            CommonTreeNodeStream treeStream = new CommonTreeNodeStream(t);
            treeStream.setTokenStream(tokens);

            if (options.printTokens) {
                System.out.println(t.toStringTree());
            }

            smaliTreeWalker dexGen = new smaliTreeWalker(treeStream);
            dexGen.setApiLevel(options.apiLevel);

            dexGen.setVerboseErrors(options.verboseErrors);
            dexGen.setDexBuilder(dexBuilder);
            dexGen.smali_file();

            //return dexGen.getNumberOfSyntaxErrors() == 0;
            if (dexGen.getNumberOfSyntaxErrors() != 0) {
                String sourceName = lexer.getSourceName();
                String errorMsg = "Source: " + sourceName + " Line: " + lexer.getLine() +
                        " Column: " + lexer.getColumn();//.getErrorMessages();
                if (sourceName.equals("")) {
                    throw new Exception("Error occurred while compiling " + smaliFile.getName());
                } else {
                    throw new Exception(errorMsg);
                }
            }

            return true;
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    private static void writeReferences(List<String> references, String filename) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(
                    new FileWriter(filename)));

            for (String reference : Ordering.natural().sortedCopy(references)) {
                writer.println(reference);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}