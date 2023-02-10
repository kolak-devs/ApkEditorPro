package com.mcal.editor.utils

import android.content.pm.PackageManager
import android.content.pm.PermissionInfo

class Permission(private val pm: PackageManager, val name: String) : Comparable<Permission> {
    private var info: PermissionInfo? = null

    var describe: String? = null
        get() {
            if (field != null) {
                return field
            }
            val loadDescription = info?.loadDescription(pm)
            if (info == null || loadDescription == null) {
                field = "No description"
                return "No description"
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

    init {
        try {
            info = pm.getPermissionInfo(name, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    fun compareTo2(permission: Permission): Int {
        return permission.label!!.compareTo(label!!)
    }

    override fun compareTo(permission: Permission): Int {
        return compareTo2(permission)
    }
}