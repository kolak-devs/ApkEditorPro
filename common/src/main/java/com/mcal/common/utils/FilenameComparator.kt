package com.mcal.common.utils

class FilenameComparator : Comparator<Any> {
    override fun compare(obj1: Any, obj2: Any): Int {
        val rec1 = obj1 as FileRecord
        val rec2 = obj2 as FileRecord
        return if (rec1.isDir) {
            if (rec2.isDir) {
                //return rec1.fileName.compareToIgnoreCase(rec2.fileName);
                myCompare(rec1.fileName, rec2.fileName)
            } else {
                -1
            }
        } else {
            if (rec2.isDir) {
                1
            } else {
                //return rec1.fileName.compareToIgnoreCase(rec2.fileName);
                myCompare(rec1.fileName, rec2.fileName)
            }
        }
    }

    companion object {
        private fun myCompare(str1: String?, str2: String?): Int {
            return if (str1.isNullOrEmpty()) {
                if (str2.isNullOrEmpty()) {
                    0
                } else {
                    -1
                }
            } else if (str2.isNullOrEmpty()) {
                1
            } else {
                val c1 = str1[0]
                val c2 = str2[0]
                if (c1 == c2) {
                    myCompare(str1.substring(1), str2.substring(1))
                } else if (isLetter(c1) && isLetter(c2)) {
                    val lower1 = c1.lowercaseChar()
                    val lower2 = c2.lowercaseChar()
                    if (lower1 == lower2) {
                        if (c1 < c2) -1 else 1
                    } else {
                        if (lower1 < lower2) -1 else 1
                    }
                } else {
                    if (c1 < c2) {
                        -1
                    } else {
                        1
                    }
                }
            }
        }

        private fun isLetter(c: Char): Boolean {
            return c in 'A'..'Z' || c in 'a'..'z'
        }
    }
}