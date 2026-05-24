package com.nowayhome.housecheck.application

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

class SafeXmlDocumentParser {
    fun parse(content: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        factory.isXIncludeAware = false
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        return factory.newDocumentBuilder()
            .parse(ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)))
    }
}

fun Document.firstText(tagName: String): String? = documentElement.firstText(tagName)

fun Document.elements(tagName: String): List<Element> {
    val nodes = getElementsByTagName(tagName)
    return (0 until nodes.length).mapNotNull { index -> nodes.item(index) as? Element }
}

fun Element.firstText(vararg tagNames: String): String? {
    for (tagName in tagNames) {
        val nodes = getElementsByTagName(tagName)
        for (index in 0 until nodes.length) {
            val text = nodes.item(index)?.textContent?.trim()
            if (!text.isNullOrBlank()) {
                return text
            }
        }
    }
    return null
}
