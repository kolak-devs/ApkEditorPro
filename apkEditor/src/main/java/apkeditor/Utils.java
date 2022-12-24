package apkeditor;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.Date;

public class Utils {

    private static final String TAG = "APKEDITOR";

    public static void showToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }

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
    public static String generateImei(int offset) {
        final long base = 35806501910400L;
        final long imei = base + offset;
        final int checksum = imei_checksum(imei);
        return String.valueOf(imei * 10 + checksum);
    }

    private static int imei_checksum(long imei) {
        final int[] sum = new int[15];
        final long mod = 10;
        for (int i = 1; i <= 14; i++) {
            sum[i] = (int) (imei % mod);
            if (i % 2 != 0) {
                sum[i] *= 2;
            }
            if (sum[i] >= 10) {
                sum[i] = sum[i] % 10 + (sum[i] / 10);
            }
            imei /= mod;
        }

        int check = 0;
        for (int j : sum) {
            check += j;
        }
        return (check * 9) % 10;
    }

    @NonNull
    private static String getValueString(Object value) {
        if (value instanceof String[]) {
            final String[] strArray = (String[]) value;
            final StringBuilder sb = new StringBuilder();
            sb.append("String[]={");
            for (int i = 0; i < strArray.length; i++) {
                sb.append("").append(i).append(":").append(strArray[i]).append(", ");
            }
            sb.append("}");
            return sb.toString();
        } else if (value instanceof Integer[]) {
            final Integer[] intArray = (Integer[]) value;
            final StringBuilder sb = new StringBuilder();
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
        final String[] buffers = {"", "  ", "    ", "      ", "        ",
                "          ", "            ", "              ",
                "                ", "                  ",
                "                    "};
        if (level < buffers.length) {
            return buffers[level];
        }

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; ++i) {
            sb.append("  ");
        }

        return sb.toString();
    }

    private static void getAllValues(@NonNull Object obj, int level, int maxlevel) {
        final Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            try {
                final Object value = field.get(obj);
                log(getPadding(level) + "Name: " + field.getName() + ", Value: " + getValueString(value));
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
        final Throwable ex = new Throwable();
        final StackTraceElement[] stackElements = ex.getStackTrace();
        if (stackElements != null) {
            for (StackTraceElement stackElement : stackElements) {
                log("\t" + stackElement.toString());
            }
        }
    }
}