package com.mcal.patchview.settings

import com.mcal.apkeditor.common.App

/**
 * Created by Snow Volf on 20.08.2017, 1:21
 */

object Preferences {
    val isArtaSyntaxAllowed: Boolean
        get() = App.getPreferences().getBoolean("ui.arta", false)

    val isMonospaceFontAllowed: Boolean
        get() = App.getPreferences().getBoolean("ui.font_monospace", true)

    var fontSize: Int
        get() {
            var size = App.getPreferences().getInt("ui.font_size", 16)
            size = Math.max(Math.min(size, 64), 8)
            return size
        }
        set(size) = App.getPreferences().edit().putInt("ui.font_size", size).apply()
}
