package com.mcal.common.utils;

import android.text.InputFilter;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Contract;

public class InputHelper {
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
