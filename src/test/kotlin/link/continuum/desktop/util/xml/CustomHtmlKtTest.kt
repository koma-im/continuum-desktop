package link.continuum.desktop.util.xml

import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.LinkTextSegment
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.PlainTextSegment
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.UserIdLink
import koma.matrix.UserId
import link.continuum.desktop.gui.message.richtext.parseRichXml
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

internal class CustomHtmlKtTest {

    @Test
    fun testDocumentBuilder() {
        val dbf = DocumentBuilderFactory.newInstance()
        val db = dbf.newDocumentBuilder()

        val s = "<a href=\"https://matrix.to/#/@john:example.org\">JohnD </a>: morning"
        val s1 = "<doc>$s</doc>"
        val d: Document = db.parse(ByteArrayInputStream(s1.toByteArray()))
        val documentElement = d.documentElement
        assertSame(documentElement, d.firstChild)

        val children: List<Node> = documentElement.childNodes.run {
            (0 until length).map { item(it) }
        }
        val link = children[0]
        assertEquals("a", link.nodeName)
        assertEquals(Node.ELEMENT_NODE, link.nodeType)
        assertEquals("JohnD ", link.textContent)
        assertNull(link.nodeValue)
        val href = link.attributes.getNamedItem("href")
        assertEquals("href", href.nodeName)
        assertEquals(Node.ATTRIBUTE_NODE, href.nodeType)
        val url = "https://matrix.to/#/@john:example.org"
        assertEquals(url, href.nodeValue)
        assertEquals(url, href.textContent)
        val text = children[1]
        assertEquals("#text", text.nodeName)
        assertEquals(Node.TEXT_NODE, text.nodeType)
        val content = ": morning"
        assertEquals(content, text.nodeValue)
        assertEquals(content, text.textContent)
    }

    @Test
    fun testParseRichText() {
        val h = "https://matrix.to/#/@john:example.org".toHttpUrlOrNull()!!
        assertEquals("/@john:example.org", h.fragment)
        assertEquals(listOf(""), h.pathSegments)
        val s = "<a href=\"https://matrix.to/#/@john:example.org\">JohnD </a>: morning"
        val richtext = s.parseRichXml()
        val link = richtext!![0]
        assert(link is UserIdLink) { "link $link"}
        link as UserIdLink
        assertEquals(UserId("@john:example.org"), link.userId)
        assertEquals("https://matrix.to/#/@john:example.org", link.url.toString())
        assertEquals("JohnD ", link.text)

        val text = richtext[1]
        assert(text is PlainTextSegment)
        text as PlainTextSegment
        assertEquals(": morning", text.text)
    }

    @Test
    fun testParseRichText1() {
        assertNull("<a href=\"https://matrix.to/#/@john:example.org\">JohnD".parseRichXml())
        assertNull("<b>".parseRichXml())
        val slice = "start <a href=\"https://matrix.to/#/@john-example.org\"></a> middle <c> end</c>"
        val parsed = slice.parseRichXml()
        assertEquals(4, parsed!!.size)
        assertEquals("start ", (parsed[0] as PlainTextSegment).text)
        val link = (parsed[1] as LinkTextSegment)
        assertEquals("", link.text)
        assertEquals("https://matrix.to/#/@john-example.org", link.url)
        assertEquals(" middle ", (parsed[2] as PlainTextSegment).text)
        assertEquals(" end", (parsed[3] as PlainTextSegment).text)
    }
}