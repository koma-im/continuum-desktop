package link.continuum.desktop.gui.message.richtext

import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.LinkTextSegment
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.PlainTextSegment
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.TextSegment
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.UserIdLink
import koma.matrix.UserId
import mu.KotlinLogging
import okhttp3.HttpUrl
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

private val logger = KotlinLogging.logger {}

private val documentBuilder by lazy {
    DocumentBuilderFactory.newInstance().newDocumentBuilder()
}

fun String.parseRichXml(): List<TextSegment>? {
    val s1 = "<doc>$this</doc>"
    val d: Document = try {
        documentBuilder.parse(ByteArrayInputStream(s1.toByteArray()))
    } catch (e: Exception) {
        logger.debug { "couldn't parse $this for $e" }
        return null
    }
    val documentElement = d.documentElement
    val children: List<Node> = documentElement.childNodes.run {
        (0 until length).map { item(it) }
    }
    return children.map { node ->
        when {
            node.nodeName == "a" && node.nodeType == Node.ELEMENT_NODE -> parseANode(node)
            node.nodeName == "#text" && node.nodeType == Node.TEXT_NODE -> PlainTextSegment(node.nodeValue?:"")
            else -> PlainTextSegment(node.textContent?:"")
        }
    }
}

private fun parseANode(node: Node): TextSegment {
    val text = node.textContent
    val plain = PlainTextSegment(text)
    val urlText = node.attributes.getNamedItem("href")?.nodeValue ?: return plain
    val userId = urlText.extractUserIdLink()
    if (userId!=null) {
        return UserIdLink(userId, text, urlText)
    }
    return LinkTextSegment(text, urlText)
}

/**
 * extract from url like "https://matrix.to/#/@john:example.org"
 */
private fun String.extractUserIdLink(): UserId? {
    val lastPath = substringAfterLast('/').substringBefore('?')
    if (lastPath.isBlank()) {
        return null
    }
    if (!lastPath.startsWith('@')) {
        return null
    }
    if (!lastPath.contains(':')) {
        return null
    }

    return UserId(lastPath)
}