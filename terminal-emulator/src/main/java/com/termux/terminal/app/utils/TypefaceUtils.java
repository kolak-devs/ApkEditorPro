package com.termux.terminal.app.utils;

import android.graphics.Typeface;

import com.termux.terminal.app.AppTerminal;

public class TypefaceUtils {

    public static Typeface quicksand() {
        return Typeface.createFromAsset(
                AppTerminal.getBaseInstance().getAssets(), "fonts/quicksand.ttf");
    }

    public static Typeface jetbrainsMono() {
        return Typeface.createFromAsset(
                AppTerminal.getBaseInstance().getAssets(), "fonts/jetbrains-mono.ttf");
    }

    public static Typeface josefinSans() {
        return Typeface.createFromAsset(
                AppTerminal.getBaseInstance().getAssets(), "fonts/josefin-sans.ttf");
    }
}