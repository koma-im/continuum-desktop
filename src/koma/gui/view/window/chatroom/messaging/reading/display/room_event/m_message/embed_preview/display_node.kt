package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview

import javafx.scene.Node
import javafx.scene.control.MenuItem
import javafx.scene.input.Clipboard
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
import koma.gui.element.emoji.icon.EmojiIcon
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.media.MediaViewers
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.site.siteViewConstructors
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.HttpUrl
import tornadofx.*
private val settings: AppSettings = appState.store.settings

sealed class FlowElement {
    abstract val node: Node
    fun isMultiLine() = this is WebContentNode && this.multiLine
}

class InlineElement(override val node: Node): FlowElement() {
    fun startsWithNewline(): Boolean = this.node is Text && this.node.text.firstOrNull() == '\n'
    fun endsWithNewline(): Boolean = this.node is Text && this.node.text.lastOrNull() == '\n'
}

fun TextSegment.toFlow(): FlowElement {
    return when(this) {
        is PlainTextSegment -> InlineElement(Text(this.text))
        is LinkTextSegment -> WebContentNode(this.text)
        is EmojiTextSegment -> makeEmojiElement(this.emoji)
    }
}

private fun makeEmojiElement(emoji: String): InlineElement {
    val icon = EmojiIcon(emoji)
    return InlineElement(icon)
}

/**
 * link with optional preview
 */
@ExperimentalCoroutinesApi
class WebContentNode(private val link: String): FlowElement() {
    override val node = VBox()
    val multiLine: Boolean

    private val menuItems = mutableListOf<MenuItem>()
    private val mediaViewers = MediaViewers()
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
            linknode.maxWidth = 200.0 * settings.scaling
            multiLine = false
        }

        setUpMenus()
    }

    private fun findPreview(): ViewNode? {
        val url = HttpUrl.parse(link)
        url ?: return null
        val site = url.host()
        val sview = siteViewConstructors.get(site)?.let { view -> view(url) }

        val filename = url.pathSegments().last()
        val ext = filename.substringAfter('.')
        val view = sview ?: mediaViewers.get(ext, url)
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

