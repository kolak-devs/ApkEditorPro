package com.mcal.apkeditor.ui.fulleditor.utils

// Return the smali root directory by smali entry
// Return like "smali" "smali_class2"
fun getSmaliRootDir(fileEntry: String): String? {
    val pos = fileEntry.indexOf('/')
    var folderName: String? = null
    if (pos != -1) {
        folderName = fileEntry.substring(0, pos)
    }
    return folderName
}

// entryName is like: smali
fun dealWithSmaliFile(
    entryName: String,
    modifiedDexNames: MutableSet<String?>
): String? {
    if ((entryName.startsWith("smali/") || entryName.startsWith("smali_"))
        && entryName.endsWith(".smali")
    ) {
        val smaliDir = getSmaliRootDir(entryName)
        modifiedDexNames.add(smaliDir)
        return smaliDir
    }
    return null
}
