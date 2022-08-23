package com.mcal.common.utilsOld;

import android.text.InputFilter;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

/**
 * Created by phe3 on 3/30/2017.
 */

public class InputUtils {
    @NonNull
    @Contract(" -> new")
    public static InputFilter getFileNameFilter() {
        return (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                switch (c) {
                    case '/':
                    case '\\':
                    case '\"':
                    case ':':
                    case '*':
                    case '?':
                    case '<':
                    case '>':
                    case '|':
                        return "";
                    default:
                        break;
                }
            }
            return null;
        };
    }
}
