package com.mcal.apkeditor.ui.fulleditor.utils

import brut.androlib.res.xml.ResXmlEncoders
import com.mcal.androlib.KXmlSerializer
import com.mcal.apkeditor.activities.types.StringItem
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

object StringsUtils {


    // Save strings to file like values-zh-rCN/strings.xml
    @JvmStatic
    @Throws(Exception::class)
    fun saveStringResource(
        decodeRootPath: String, qualifier: String,
        valueList: List<StringItem>
    ) {
        val fileName = "strings.xml"
        val xmlSerializer: XmlSerializer = KXmlSerializer()
        val dirPath = "$decodeRootPath/res/values$qualifier"
        val dirFile = File(dirPath)
        if (!dirFile.exists()) {
            dirFile.mkdirs()
        }
        val file = File("$dirPath/$fileName")
        val fos = FileOutputStream(file)
        val writer = OutputStreamWriter(fos)
        xmlSerializer.setOutput(writer)
        writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        writer.write("<resources>\n")

        // LOGGER.info(path);
        for (v in valueList) {
            xmlSerializer.startTag(null, "string")
            xmlSerializer.attribute(null, "name", v.name)
            if (ResXmlEncoders.hasMultipleNonPositionalSubstitutions(v.value)) {
                xmlSerializer.attribute(null, "formatted", "false")
            }
            // Special case: reference, no encode needed
            val txt: String? = if (v.value.startsWith("@string/") || v.value.startsWith("@android:string/")
            ) {
                v.value
            } else {
                if (v.styledValue == null) {
                    val escaped = ResXmlEncoders.escapeXmlChars(v.value)
                    ResXmlEncoders.encodeAsXmlValue(escaped)
                } else {
                    ResXmlEncoders.encodeAsXmlValue(v.styledValue)
                }
            }
            xmlSerializer.ignorableWhitespace(txt)
            xmlSerializer.endTag(null, "string")
            xmlSerializer.flush()
            writer.write("\n")
        }
        writer.write("</resources>\n")
        writer.close()
        fos.close()
    }

    // Merge changed string values to allStringValues
    @JvmStatic
    fun mergeStrings(
        allStringValues: Map<String?, ArrayList<StringItem>?>,
        changedStringValues: Map<String, Map<String, String>>
    ) {
        val entries = changedStringValues.entries
        for ((cfgFlags, values) in entries) {
            for ((key, value) in values) {
                val merged = allStringValues[cfgFlags]
                // find the key in merged record
                if (merged != null) {
                    for (rec in merged) {
                        if (key == rec.name) {
                            rec.value = value
                            break
                        }
                    }
                }
            }
        }
    }
}