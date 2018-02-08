package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview

import javafx.scene.Node
import javafx.scene.control.MenuItem
import javafx.scene.input.Clipboard
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.media.mediaViewConstructors
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.site.siteViewConstructors
import koma.storage.config.settings.AppSettings
import tornadofx.*

sealed class FlowElement {
    abstract val node: Node
    fun isMultiLine() = this is WebContentNode && this.multiLine
}

class InlineElement(override val node: Text): FlowElement()

fun TextSegment.toFlow(): FlowElement {
    return when(this.kind) {
        TextSegmentKind.Plain -> InlineElement(Text(this.text))
        TextSegmentKind.Link -> WebContentNode(this.text)
    }
}

/**
 * link with optional preview
 */
class WebContentNode(private val link: String): FlowElement() {
    override val node = VBox()
    val multiLine: Boolean

    private val menuItems = mutableListOf<MenuItem>()

    init {
        val linknode = hyperlinkNode(link)
        node.add(linknode)

        val preview = findPreview()
        if (preview != null) {
            multiLine = true
            val prefWide = doubleBinding(preview.node.widthProperty()) { Math.max(value, 160.0)}
            linknode.prefWidthProperty().bind(prefWide)
            setUpPrevie(preview)
        } else {
            linknode.maxWidth = 200.0 * AppSettings.scaling
            multiLine = false
        }

        setUpMenus()
    }

    private fun findPreview(): ViewNode? {
        val site = link.substringAfter("://").substringBefore('/')
        val sview = siteViewConstructors.get(site)?.let { view -> view(link) }

        val ext = link.substringAfterLast('/').substringAfter('.')
        val view = sview ?: mediaViewConstructors.get(ext)?.let { vc -> vc(link) }
        return view
    }

    private fun setUpPrevie(view: ViewNode) {
        node.style {
            borderColor = multi(box(Color.LIGHTGRAY))
            borderWidth = multi(box(0.1.em))
            borderRadius = multi(box(0.5.em))
            backgroundRadius = multi(box(0.5.em))
        }

        node.add(view.node)

        menuItems.addAll(view.menuItems)
    }

    private fun setUpMenus() {
        node.lazyContextmenu {
            this.items.addAll(menuItems)
            item("Copy URL").action { Clipboard.getSystemClipboard().putString(link) }
            item("Open in Browser").action { openInBrowser(link) }
        }
    }
}

