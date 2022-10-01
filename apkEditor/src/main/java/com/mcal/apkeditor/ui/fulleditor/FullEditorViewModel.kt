package com.mcal.apkeditor.ui.fulleditor

import androidx.lifecycle.ViewModel

class FullEditorViewModel : ViewModel() {
    var decodedPath: String? = null
    var apkPath: String? = null
    var isFullDecoding = false
    var fileEntry2ZipEntry: MutableMap<String, String>? = null
    var decodedFailed: String? = null
    var isResTableDecoded = false

    var manifestModified = false

    // Strings
    var curConfig: String? = null
    var allStringValuesFile: String? = null
    var changedStringValuesFile: String? = null
}