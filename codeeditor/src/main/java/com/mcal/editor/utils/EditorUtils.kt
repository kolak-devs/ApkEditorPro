package com.mcal.editor.utils

import com.mcal.common.App
import com.mcal.common.data.ReactivePreferences.isNightModeAsync
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import org.eclipse.tm4e.core.registry.IGrammarSource
import org.eclipse.tm4e.core.registry.IThemeSource

object EditorUtils {
    val javaLanguage: Language
        get() = TextMateLanguage.create(
            "source.java", false
        )

    val luaLanguage: Language
        get() = TextMateLanguage.create(
            "source.lua", false
        )

    val jsonLanguage: Language
        get() = TextMateLanguage.create(
            "source.json", false
        )

    val kotlinLanguage: Language
        get() = TextMateLanguage.create(
            "source.kotlin", false
        )

    val pythonLanguage: Language
        get() = TextMateLanguage.create(
            "source.python", false
        )

    val htmlLanguage: Language
        get() = TextMateLanguage.create(
            "text.html.basic", false
        )

    val javascriptLanguage: Language
        get() = TextMateLanguage.create(
            "source.js", false
        )

    val markdownLanguage: Language
        get() = TextMateLanguage.create(
            "text.html.markdown", false
        )

    val groovyLanguage: Language
        get() = TextMateLanguage.create(
            "source.groovy", false
        )

    val xmlLanguage: Language
        get() = TextMateLanguage.create(
            "text.xml", false
        )

    val smaliLanguage: Language
        get() = TextMateLanguage.create(
            "source.smali", false
        )

    fun getTextMateLanguage(name: String, path: String): TextMateLanguage? {
        return try {
            TextMateLanguage.create(
                IGrammarSource.fromInputStream(
                    App.getContext().assets.open(path),
                    name,
                    null
                ),
                null,
                if (isNightModeAsync()) {
                    getDarkTheme()
                } else {
                    getLightTheme()
                }
            )
        } catch (e: java.lang.Exception) {
            throw RuntimeException(e)
        }
    }

    fun getCodeColorScheme(): TextMateColorScheme {
        try {
            val registry = ThemeRegistry.getInstance()
            if (isNightModeAsync()) {
                registry.setTheme("darcula")
            } else {
                registry.setTheme("quietlight")
            }
            return TextMateColorScheme.create(registry)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    private fun getDarkTheme(): IThemeSource? {
        return try {
            IThemeSource.fromInputStream(
                App.getContext().assets.open("textmate/darcula.json"),
                "darcula.json",
                null
            )
        } catch (e: java.lang.Exception) {
            throw RuntimeException(e)
        }
    }

    private fun getLightTheme(): IThemeSource? {
        return try {
            IThemeSource.fromInputStream(
                App.getContext().assets.open("textmate/quietlight.json"),
                "quietlight.json",
                null
            )
        } catch (e: java.lang.Exception) {
            throw RuntimeException(e)
        }
    }
}