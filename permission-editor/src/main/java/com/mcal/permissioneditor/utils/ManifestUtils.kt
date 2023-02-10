package com.mcal.permissioneditor.utils

import com.mcal.permissioneditor.model.Permission
import org.w3c.dom.Document
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList

class ManifestUtils(private val permission: List<Permission>, document: Document, private val isAnnotation: Boolean) {
    private val sb = StringBuffer()
    private var isAdded = false

    init {
        sb.append("<?xml version='1.0' encoding='utf-8'?>")
        loadNodeList(sb, document.childNodes, null, 0)
    }

    private fun getSpace(i: Int): String {
        val stringBuffer = StringBuilder()
        for (i2 in 0 until i) {
            stringBuffer.append(" ")
        }
        return stringBuffer.toString()
    }

    private fun loadNodeAttributes(stringBuffer: StringBuffer, namedNodeMap: NamedNodeMap?, str: String) {
        if (namedNodeMap != null) {
            val length = namedNodeMap.length
            for (i in 0 until length) {
                val item = namedNodeMap.item(i)
                val nodeName = item.nodeName
                val nodeValue = item.nodeValue
                if (length > 1) {
                    stringBuffer.append("\n")
                    stringBuffer.append(str)
                } else if (length > 0) {
                    stringBuffer.append(" ")
                }
                stringBuffer.append(nodeName)
                stringBuffer.append("=\"")
                stringBuffer.append(nodeValue)
                stringBuffer.append("\"")
            }
        }
    }

    private fun loadNodeList(stringBuffer: StringBuffer, nodeList: NodeList, node: Node?, i: Int) {
        val length = nodeList.length
        for (i2 in 0 until length) {
            val item = nodeList.item(i2)
            val nodeName = item.nodeName
            val attributes = item.attributes
            if (!nodeName.startsWith("#") && (isAdded || node != null && "manifest" != node.nodeName || "uses-permission" != nodeName || attributes.length > 1)) {
                val childNodes = item.childNodes
                val space = getSpace(i)
                val space2 = getSpace(indent + i)
                if (i > 0) {
                    stringBuffer.append("\n\n")
                } else {
                    stringBuffer.append("\n")
                }
                if (permission.isNotEmpty() && !isAdded && node != null && "manifest" == node.nodeName) {
                    for (permission in permission) {
                        if (isAnnotation) {
                            stringBuffer.append(space)
                            stringBuffer.append("<!--")
                            stringBuffer.append(permission.label)
                            stringBuffer.append("-->\n")
                        }
                        stringBuffer.append(space)
                        stringBuffer.append("<uses-permission android:name=\"")
                        stringBuffer.append(permission.name)
                        stringBuffer.append("\"/>")
                        stringBuffer.append("\n\n")
                    }
                    isAdded = true
                }
                if (i > 0) {
                    stringBuffer.append(space)
                }
                stringBuffer.append("<")
                stringBuffer.append(nodeName)
                loadNodeAttributes(stringBuffer, attributes, space2)
                if (childNodes.length == 0) {
                    stringBuffer.append("/>")
                } else {
                    stringBuffer.append(">")
                    loadNodeList(stringBuffer, childNodes, item, indent + i)
                    stringBuffer.append("\n\n")
                    if (i > 0) {
                        stringBuffer.append(space)
                    }
                    stringBuffer.append("</")
                    stringBuffer.append(nodeName)
                    stringBuffer.append(">")
                }
            }
        }
    }

    fun result(): String {
        return sb.toString()
    }

    companion object {
        private const val indent = 4
        fun save(list: List<Permission>, document: Document, z: Boolean): String {
            return ManifestUtils(list, document, z).result()
        }
    }
}