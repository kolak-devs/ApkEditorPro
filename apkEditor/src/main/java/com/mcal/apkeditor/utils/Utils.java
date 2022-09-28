package com.mcal.apkeditor.utils;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.mcal.apkeditor.BuildConfig;

import java.lang.reflect.Field;
import java.util.Date;

public class Utils {

    private static final String TAG = "APKEDITOR";

    public static void log(String msg) {
        Log.d(TAG, msg);
    }

    public static void dumpValue(Object obj) {
        if (obj != null) {
            log("Values of " + obj + ":");
            getAllValues(obj, 1, 3);
        } else {
            log("null");
        }
    }

    public static String stringAdd1(String str) {
        if (str != null && !str.equals("")) {
            char c = str.charAt(str.length() - 1);
            c += 1;
            return str.substring(0, str.length() - 1) + c;
        }
        return str;
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

    private static void getAllValues(@NonNull Object obj, int level, int maxlevel) {

        Field[] fields = obj.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {

            if (!fields[i].isAccessible()) {
                fields[i].setAccessible(true);
            }

            try {
                Object value = fields[i].get(obj);
                log(getPadding(level) + "Name: " + fields[i].getName()
                        + ", Value: " + getValueString(value));
                if (level < maxlevel && value != null && !isBasicType(value)) {
                    getAllValues(value, level + 1, maxlevel);
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isBasicType(Object param) {
        if (param instanceof Integer) {
            return true;
        } else if (param instanceof String) {
            return true;
        } else if (param instanceof Double) {
            return true;
        } else if (param instanceof Float) {
            return true;
        } else if (param instanceof Long) {
            return true;
        } else if (param instanceof Boolean) {
            return true;
        } else if (param instanceof Date) {
            return true;
        } else if (param instanceof Integer[]) {
            return true;
        } else if (param instanceof String[]) {
            return true;
        } else if (param instanceof Double[]) {
            return true;
        } else if (param instanceof Float[]) {
            return true;
        } else if (param instanceof Long[]) {
            return true;
        } else if (param instanceof Boolean[]) {
            return true;
        } else return param instanceof Date[];
    }

    public static void printCallStack() {
        printCallStack(null);
    }

    public static void printCallStack(String tag) {
        if (tag != null) {
            log("Stack at " + tag + ": ");
        } else {
            log("Stack:");
        }
        Throwable ex = new Throwable();
        StackTraceElement[] stackElements = ex.getStackTrace();
        if (stackElements != null) {
            for (int i = 0; i < stackElements.length; i++) {
                log("\t" + stackElements[i].toString());
            }
        }
    }

    public static String getVersionString(){
        return "v. " + BuildConfig.VERSION_NAME + " [" + Build.SUPPORTED_ABIS[0] + "]";
    }
}
