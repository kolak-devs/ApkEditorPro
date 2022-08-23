package com.mcal.common.utilsOld;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ActivityUtils {

    @NonNull
    public static Bundle attachParam(@NonNull Intent intent, String key, String value) {
        Bundle bundle = new Bundle();
        bundle.putString(key, value);
        intent.putExtras(bundle);
        return bundle;
    }

    @NonNull
    public static Bundle attachParam(@NonNull Intent intent, String key, int[] value) {
        Bundle bundle = new Bundle();
        bundle.putIntArray(key, value);
        intent.putExtras(bundle);
        return bundle;
    }

    @NonNull
    public static Bundle attachParam(@NonNull Intent intent, String key, boolean value) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(key, value);
        intent.putExtras(bundle);
        return bundle;
    }

    @NonNull
    public static Bundle attachParam(@NonNull Intent intent, String key, ArrayList<String> value) {
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(key, value);
        intent.putExtras(bundle);
        return bundle;
    }

    @NonNull
    public static Bundle attachParam2(@NonNull Intent intent, String key, ArrayList<Integer> value) {
        Bundle bundle = new Bundle();
        bundle.putIntegerArrayList(key, value);
        intent.putExtras(bundle);
        return bundle;
    }

    @NonNull
    public static Bundle attachParam(@NonNull Intent intent, String key, int value) {
        Bundle bundle = new Bundle();
        bundle.putInt(key, value);
        intent.putExtras(bundle);
        return bundle;
    }

    @NonNull
    public static Bundle attachBoolParam(@NonNull Intent intent, String key, boolean value) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(key, value);
        intent.putExtras(bundle);
        return bundle;
    }

    @Nullable
    public static String getParam(@NonNull Intent intent, String key) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            return bundle.getString(key);
        }
        return null;
    }

    public static boolean getBoolParam(@NonNull Intent intent, String key) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            return bundle.getBoolean(key, false);
        }
        return false;
    }

    public static int getIntParam(@NonNull Intent intent, String key) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            return bundle.getInt(key, 0);
        }
        return 0;
    }

    @Nullable
    public static ArrayList<String> getStringArray(@NonNull Intent intent, String key) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            return bundle.getStringArrayList(key);
        }
        return null;
    }

    @Nullable
    public static ArrayList<Integer> getIntArray(@NonNull Intent intent, String key) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            return bundle.getIntegerArrayList(key);
        }
        return null;
    }

    public static void attachParam(Intent intent, String key,
                                   @NonNull Map<String, String> mapValue) {
        Bundle bundle = new Bundle();
        ArrayList<String> keyList = new ArrayList<String>();
        ArrayList<String> valueList = new ArrayList<String>();
        for (String strKey : mapValue.keySet()) {
            String strVal = mapValue.get(strKey);
            keyList.add(strKey);
            valueList.add(strVal);
        }
        bundle.putStringArrayList(key + "_keys", keyList);
        bundle.putStringArrayList(key + "_values", valueList);
        intent.putExtras(bundle);
    }

    @Nullable
    public static Map<String, String> getMapParam(@NonNull Intent intent, String key) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Map<String, String> mapVal = new HashMap<String, String>();
            ArrayList<String> strKeys = bundle
                    .getStringArrayList(key + "_keys");
            ArrayList<String> strValues = bundle.getStringArrayList(key
                    + "_values");
            if (strKeys != null && strValues != null) {
                for (int i = 0; i < strKeys.size(); i++) {
                    mapVal.put(strKeys.get(i), strValues.get(i));
                }
                return mapVal;
            }
        }
        return null;
    }
}
