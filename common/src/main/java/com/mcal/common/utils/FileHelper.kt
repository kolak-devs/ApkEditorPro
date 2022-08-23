package com.mcal.common.utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

private fun Path.exists(): Boolean = Files.exists(this)

private fun Path.isFile(): Boolean = !Files.isDirectory(this)

fun Path.move(dest: Path, overwrite: Boolean = false): Boolean {
    return if (isFile()) {
        if (dest.exists()) {
            if (overwrite) {
                //Perform the move operation. REPLACE_EXISTING is needed for
                //replacing a file
                Files.move(this, dest, StandardCopyOption.REPLACE_EXISTING)
                true
            } else {
                false
            }
        } else {
            //Perform the move operation
            Files.move(this, dest)
            true
        }
    } else {
        false
    }
}