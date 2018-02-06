package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview

import javafx.scene.Node
import javafx.scene.input.Clipboard
import javafx.scene.text.Text
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.media.mediaViewConstructors
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.site.siteViewConstructors
import tornadofx.*

fun TextElement.toNode(): Node {
    return when(this.kind) {
        TextElementKind.Plain -> Text(this.text)
        TextElementKind.Link -> webContentNode(this.text)
    }
}

fun webContentNode(link: String): Node {
    val site = link.substringAfter("://").substringBefore('/')
    val sview = siteViewConstructors.get(site)?.let { view -> view(link) }

    val ext = link.substringAfterLast('/').substringAfter('.')
    val view = sview ?: mediaViewConstructors.get(ext)?.let { vc -> vc(link) }

    val node = view?.node ?: hyperlinkNode(link)
    node.lazyContextmenu {
        view?.let { this.items.addAll(it.menuItems) }
        item("Copy URL").action { Clipboard.getSystemClipboard().putString(link) }
        item("Open in Browser").action { openInBrowser(link) }
    }
    return node
}

