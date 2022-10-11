package com.mcal.common.utils

class FileRecord {
    @JvmField
    var fileName: String? = null

    @JvmField
    var isDir = false

    @JvmField
    var totalSize: Long = 0

    @JvmField
    var isInZip = false

    @JvmField
    var size = 0

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