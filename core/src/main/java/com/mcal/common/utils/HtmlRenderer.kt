package com.mcal.common.utils

import com.mcal.common.App
import com.mcal.common.data.LegacyPreferences
import com.mcal.common.data.ReactivePreferences
import org.jetbrains.annotations.Contract

object HtmlRenderer {
    @JvmStatic
    suspend fun renderHtml(html: String): String {
        return html
            .replace("<head>", "<head>${style()}")
            .replace(
                "androidstudio.css",
                if (ReactivePreferences.isNightMode())
                    "darkcode.css"
                else "androidstudio.css"
            )
            .replace("<body>", "<body>$translatePlugin")
            .replace("<body>", if (ReactivePreferences.isNightMode()) "<body style='${darkMode()}'>" else "<body>")
    }

    private suspend fun style(): String {
        return StringBuilder().append("<style>@font-face{font-family:CustomFont; src:url(file:///android_asset/JetBrainsMono-Regular.ttf);}")
            .append("p, h1, h2, h3, table, ul, ol {font-size:" + ReactivePreferences.getFontSize() + "; font-family:CustomFont;}")
            .append("pre,code {font-size:" + ReactivePreferences.getFontSize() + "; font-family:CustomFont;}")
            .append(".goog-te-banner-frame{display:none;}")
            .append("")
            .append(darkMode())
            .append("</style>")
            .toString()
    }

    private suspend fun darkMode(): String {
        return if (ReactivePreferences.isNightMode()){
            "background:#323232; color:#FAFAFA;"
        } else ""
    }


    private val translatePlugin: String
        get() = FileReader.fromAssets("translate/google.html")
}
