package com.mcal.apkeditor.utils;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.BuildConfig;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import kotlin.io.FilesKt;

public class Utils {

    private static final String TAG = "APKEDITOR";

    public static void log(String msg) {
        Log.d(TAG, msg);
    }

    @NonNull
    private static String getValueString(Object value) {
        if (value instanceof String[]) {
            String[] strArray = (String[]) value;
            StringBuilder sb = new StringBuilder();
            sb.append("String[]={");
            for (int i = 0; i < strArray.length; i++) {
                sb.append("").append(i).append(":").append(strArray[i]).append(", ");
            }
            sb.append("}");
            return sb.toString();
        } else if (value instanceof Integer[]) {
            Integer[] intArray = (Integer[]) value;
            StringBuilder sb = new StringBuilder();
            sb.append("Integer[]={");
            for (int i = 0; i < intArray.length; i++) {
                sb.append("").append(i).append(":").append(intArray[i]).append(", ");
            }
            sb.append("}");
            return sb.toString();
        }
        return value == null ? "null" : value.toString();
    }

    private static String getPadding(int level) {
        String[] buffers = {"", "  ", "    ", "      ", "        ",
                "          ", "            ", "              ",
                "                ", "                  ",
                "                    "};
        if (level < buffers.length) {
            return buffers[level];
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; ++i) {
            sb.append("  ");
        }

        return sb.toString();
    }

    @NonNull
    @Contract(pure = true)
    public static String getVersionString(){
        return "v. " + BuildConfig.VERSION_NAME + " [" + Build.SUPPORTED_ABIS[0] + "]";
    }

    public static long getFoldersSize(@NonNull File... folders){
        long commonSize = 0;
        for (File folder: folders){
            commonSize += sizeFromPath(folder.toPath());
        }
        return commonSize;
    }

    /**
     * Attempts to calculate the size of a file or directory.
     *
     * <p>
     * Since the operation is non-atomic, the returned value may be inaccurate.
     * However, this method is quick and does its best.
     */
    public static long sizeFromPath(Path path) {

        final AtomicLong size = new AtomicLong(0);

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {

                    Log.i("Apk Editor", "skipped: " + file + " (" + exc + ")");
                    // Skip folders that can't be traversed
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                    if (exc != null)
                        System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
                    // Ignore errors traversing a folder
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
        }

        return size.get();
    }

    //delete folders
    public static void deleteFiles(@NonNull File... files){
        for (File fs : files){
            FilesKt.deleteRecursively(fs);
        }
    }
}
