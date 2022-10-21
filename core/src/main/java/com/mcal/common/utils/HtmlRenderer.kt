package com.mcal.common.utils

import com.mcal.common.data.Preferences
import org.jetbrains.annotations.Contract

object HtmlRenderer {
    @JvmStatic
    fun renderHtml(html: String): String {
        return html
            .replace("<head>", "<head>$style")
            .replace(
                "androidstudio.css",
                if (Preferences.isNightModeEnabled()) "darkcode.css" else "androidstudio.css"
            )
            .replace("<body>", "<body>$translatePlugin")
            .replace("<body>", if (Preferences.isNightModeEnabled()) "<body style='$darkMode'>" else "<body>")
    }

    private val style: String
        get() = ("<style>@font-face{font-family:CustomFont; src:url(file:///android_asset/JetBrainsMono-Regular.ttf);}"
                + "p, h1, h2, h3, table, ul, ol {font-size:" + Preferences.getFontSize() + "; font-family:CustomFont;}"
                + "pre,code {font-size:" + Preferences.getFontSize() + "; font-family:CustomFont;}"
                + ".goog-te-banner-frame{display:none;}"
                + if (Preferences.isNightModeEnabled()) "$darkMode</style>" else "</style>")

    @get:Contract(pure = true)
    private val darkMode: String
        get() = "background:#323232; color:#FAFAFA;"

    private val translatePlugin: String
        get() = FileReader.fromAssets("translate/google.html")
}