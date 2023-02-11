package com.mcal.permissioneditor.utils

import android.content.Context
import com.mcal.permissioneditor.model.Permission
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

object ManifestParser {
    @JvmStatic
    @Throws(ParserConfigurationException::class, IOException::class, SAXException::class)
    fun load(context: Context, file: File): MutableList<Permission> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val elementsByTagName = document.getElementsByTagName("manifest")
        if (elementsByTagName.length == 0) {
            throw SAXException("No manifest node found")
        }
        val nodeList = elementsByTagName.item(0).childNodes
        val permissions = mutableListOf<Permission>()
        for (i in 0 until nodeList.length) {
            var item = nodeList.item(i)
            val attributes = item.attributes
            if ("uses-permission" == item.nodeName && attributes.length == 1) {
                for (i2 in 0 until attributes.length) {
                    item = attributes.item(i2)
                    if ("android:name" == item.nodeName) {
                        val nodeValue = item.nodeValue
                        if (nodeValue.trim().isNotEmpty()) {
                            permissions.add(Permission(context, context.packageManager, nodeValue))
                        }
                    }
                }
            }
        }
        return permissions
    }
}