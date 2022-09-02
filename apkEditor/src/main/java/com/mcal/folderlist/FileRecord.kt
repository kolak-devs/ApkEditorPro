package com.mcal.folderlist

class FileRecord {
    @JvmField
    var fileName: String? = null
    @JvmField
    var isDir = false
    @JvmField
    var totalSize: Long = 0
    @JvmField
    var isInZip = false

    constructor()
    constructor(fileName: String?, isDir: Boolean, size: Long) : this(fileName, isDir, false, size) {}

    @JvmOverloads
    constructor(fileName: String?, isDir: Boolean, isInZip: Boolean = false, size: Long = 0) {
        this.fileName = fileName
        this.isDir = isDir
        this.isInZip = isInZip
        totalSize = size
    }
}