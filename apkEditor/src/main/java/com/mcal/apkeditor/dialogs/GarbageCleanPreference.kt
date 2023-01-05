package com.mcal.apkeditor.dialogs

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.util.Log
import androidx.preference.MultiSelectListPreference
import com.mcal.common.utils.ScopedStorage

class GarbageCleanPreference : MultiSelectListPreference {
    private val TAG = "GarbageCleanPreference"

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {}

    fun initialize() {}

    fun onClick(dialog: DialogInterface, which: Int) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            val entries = sharedPreferences?.getStringSet(key, HashSet())
            for (option in entries!!) {
                if (option.equals("temp")) {
                    ScopedStorage.cacheDir.deleteRecursively()
                    ScopedStorage.getDecodedDir().deleteRecursively()
                    ScopedStorage.getTmpDir().deleteRecursively()
                    Log.d(TAG, "onClick: temp")
                } else if (option.equals("signing")) {
                    ScopedStorage.getKeysDir().deleteRecursively()
                    Log.d(TAG, "onClick: signing")
                } else if (option.equals("projects")) {
                    ScopedStorage.getProjects().deleteRecursively()
                    Log.d(TAG, "onClick: projects")
                } else if (option.equals("backups")) {
                    ScopedStorage.getBackupsDir().deleteRecursively()
                    Log.d(TAG, "onClick: backups")
                }
            }
        }
        //super.onClick(dialog, which)
    }
}