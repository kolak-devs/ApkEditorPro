package com.mcal.permissioneditor.model

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import com.mcal.neweditor.R

class Permission(private val context: Context, private val pm: PackageManager, val name: String) : Comparable<Permission> {
    private var info: PermissionInfo? = null

    init {
        try {
            info = pm.getPermissionInfo(name, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    var describe: String? = null
        get() {
            if (field != null) {
                return field
            }
            val loadDescription = info?.loadDescription(pm)
            if (info == null || loadDescription == null) {
                val text = context.getString(R.string.no_description)
                field = text
                return text
            }
            val charSequence = loadDescription.toString()
            field = charSequence
            return charSequence
        }


    var label: String? = null
        get() {
            if (field != null) {
                return field
            }
            val loadLabel = info?.loadLabel(pm)
            if (info == null || loadLabel == null || loadLabel.toString() == name) {
                val lastIndexOf = name.lastIndexOf(".")
                if (lastIndexOf >= 0) {
                    val substring = name.substring(lastIndexOf + 1)
                    field = substring
                    return substring
                }
                val str = name
                field = str
                return str
            }
            val charSequence = loadLabel.toString()
            field = charSequence
            return charSequence
        }

    override fun compareTo(other: Permission): Int {
        return label?.let { it -> other.label?.compareTo(it) } ?: 0
    }
}