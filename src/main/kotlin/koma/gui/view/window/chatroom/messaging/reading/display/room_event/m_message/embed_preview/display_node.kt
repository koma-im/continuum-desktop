package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview

import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Text
import koma.Server
import koma.gui.element.emoji.icon.EmojiIcon
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.media.MediaViewers
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.site.siteViewConstructors
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.action
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.doubleBinding
import link.continuum.desktop.gui.item
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

private val settings: AppSettings = appState.store.settings

sealed class FlowElement {
    abstract val node: Node
    fun isMultiLine() = this is WebContentNode && this.multiLine
}

class InlineElement(override val node: Node): FlowElement() {
    fun startsWithNewline(): Boolean = this.node is Text && this.node.text.firstOrNull() == '\n'
    fun endsWithNewline(): Boolean = this.node is Text && this.node.text.lastOrNull() == '\n'
}

fun messageSliceView(slice: TextSegment, server: Server): FlowElement {
    return when(slice) {
        is PlainTextSegment -> InlineElement(Text(slice.text))
        is LinkTextSegment -> WebContentNode(slice.text, server)
        is EmojiTextSegment -> {
            val icon = EmojiIcon(slice.emoji, settings.fontSize)
            InlineElement(icon.node)
        }
        is UserIdLink -> InlineElement(Text(slice.text))
    }
}


/**
 * link with optional preview
 */
@ExperimentalCoroutinesApi
class WebContentNode(private val link: String,
                     server: Server
): FlowElement() {
    override val node = VBox()
    val multiLine: Boolean

    private val menuItems = mutableListOf<MenuItem>()
    private val mediaViewers = MediaViewers(server)
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
        node.border = Border(BorderStroke(
                Color.LIGHTGRAY,
                BorderStrokeStyle.SOLID,
                CornerRadii(0.5, true),
                BorderWidths(1.0)
        ))
        node.background = Background(BackgroundFill(
                Color.TRANSPARENT,
                CornerRadii(0.5, true),
                Insets.EMPTY))

        node.add(view.node)

        menuItems.addAll(view.menuItems)
    }

    private val cmenu by lazy {
        ContextMenu().apply {
            this.items.addAll(menuItems)
            item("Copy URL").action {
                Clipboard.getSystemClipboard().setContent(
                        ClipboardContent().apply {
                            putString(link)
                        }
                )
            }
            item("Open in Browser").action { openInBrowser(link) }
        }
    }
    private fun setUpMenus() {
        node.setOnContextMenuRequested {
            cmenu.show(this.node, it.screenX, it.screenY)
            it.consume()
        }
    }
}

