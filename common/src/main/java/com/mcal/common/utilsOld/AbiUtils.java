package com.mcal.common.utilsOld;

import android.os.Build;

public class AbiUtils {
    public static String getAbi() {
        String result = getAllAbi();

        if(getAllAbi().contains("arm64")) {
            result = "arm64-v8a";
        } else if(getAllAbi().contains("armeabi-v7a")) {
            result = "arm64-v8a";
        } else if(getAllAbi().contains("x86_64")) {
            result = "x86_64";
        } else if(getAllAbi().contains("x86")) {
            result = "x86";
        }
        return result;
    }

    public static String getAllAbi() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS[0];
        } else {
            return Build.CPU_ABI;
        }
    }
}
