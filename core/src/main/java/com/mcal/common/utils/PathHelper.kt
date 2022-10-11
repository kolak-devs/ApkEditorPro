package com.mcal.common.utils

import java.io.File

fun getNameFromPath(path: String?): String? {
    if (path != null) {
        val pos = path.lastIndexOf('/')
        return path.substring(pos + 1)
    }
    return null
}

fun replaceNameWith(path: String?, newName: String): String? {
    if (path != null) {
        val pos = path.lastIndexOf('/')
        return path.substring(0, pos + 1) + newName
    }
    return null
}

// Check if path1 is the parent folder of path2
fun isParentFolderOf(folderPath1: String, folderPath2: String): Boolean {
    var path1 = folderPath1
    var path2 = folderPath2
    if (path1.endsWith("/")) {
        path1 = path1.substring(0, path1.length - 1)
    }
    if (path2.endsWith("/")) {
        path2 = path2.substring(0, path2.length - 1)
    }
    val p1 = path1.split("/").toTypedArray()
    val p2 = path2.split("/").toTypedArray()
    if (p1.size < p2.size) {
        for (i in p1.indices) {
            if (p1[i] != p2[i]) {
                return false
            }
        }
        return true
    }
    return false
}

// Return "0" if parent=/storage/emulated, referPath=/storage/emulated/0
// Call isParentFolderOf before calling this function
fun getSubFolder(parentPath: String, referPath: String): String? {
    var parent = parentPath
    var path = referPath
    if (parent.endsWith("/")) {
        parent = parent.substring(0, parent.length - 1)
    }
    if (path.endsWith("/")) {
        path = path.substring(0, path.length - 1)
    }
    val p1 = parent.split("/").toTypedArray()
    val p2 = path.split("/").toTypedArray()
    return if (p1.size < p2.size) {
        p2[p1.size]
    } else null
}

fun getDirectoryPath(filePath: String): String {
    val f = File(filePath)
    if (f.isDirectory) {
        return f.path
    } else {
        val pos = filePath.lastIndexOf('/')
        if (pos != -1) {
            return filePath.substring(0, pos)
        }
    }
    return filePath
}

// Get parent folder name for a file
fun getParentFolder(filePath: String): String {
    val names = filePath.split("/").toTypedArray()
    // Maybe the file is in the root directory like "/etc"
    return if (names.size < 2) {
        ""
    } else {
        names[names.size - 2]
    }
}

// Get the target saving file/dir which not exist, by adding (1), (2), etc
fun getTargetNonExistFile(path: String, isDir: Boolean): File {
    var index = 1
    var folder: String? = null
    var name: String? = null
    var fileType: String? = null
    if (!isDir) {
        val slashPos = path.lastIndexOf('/')
        folder = path.substring(0, slashPos + 1)
        val filename = path.substring(slashPos + 1)
        name = filename
        fileType = ""
        val dotPos = filename.lastIndexOf('.')
        if (dotPos != -1) {
            name = filename.substring(0, dotPos)
            fileType = filename.substring(dotPos)
        }
    }
    while (true) {
        val testingPath = if (isDir) {
            "$path($index)"
        } else {
            folder + name + index + fileType
        }
        val node = File(testingPath)
        if (!node.exists()) {
            return node
        }
        index += 1
    }
}