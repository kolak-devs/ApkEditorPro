package com.mcal.apkeditor.smali

interface ISmaliAssembleCallback {
    fun updateAssembledFiles(assembledFiles: Int, totalFiles: Int)
}