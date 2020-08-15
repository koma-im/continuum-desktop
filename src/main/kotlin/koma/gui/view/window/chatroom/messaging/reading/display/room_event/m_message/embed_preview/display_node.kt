@file:Suppress("EXPERIMENTAL_API_USAGE")

package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.*
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
import koma.Server
import koma.gui.element.emoji.icon.EmojiIcon
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.media.MediaViewers
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.site.siteViewConstructors
import koma.koma_app.appState
import koma.matrix.UserId
import koma.storage.persistence.settings.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.HBox
import link.continuum.desktop.gui.icon.avatar.AvatarInline
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.util.debugAssert
import link.continuum.desktop.util.debugAssertUiThread
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private val settings: AppSettings = appState.store.settings

sealed class FlowElement {
    abstract val node: Node
    fun isMultiLine() = this is WebContentNode && this.multiLine
}

class InlineElement(override val node: Node): FlowElement() {
    fun startsWithNewline(): Boolean = this.node is Text && this.node.text.firstOrNull() == '\n'
    fun endsWithNewline(): Boolean = this.node is Text && this.node.text.lastOrNull() == '\n'
}

class UserLinkElement(
        private val userDatas: UserDataStore
): FlowElement() {
    private val scope = MainScope()
    private val channel = Channel<Pair<UserId, Server>>(Channel.CONFLATED)
    private val avatar = AvatarInline().apply {
        root.background = whiteBackground
    }
    private val label = Text().apply {
        fill = Color.WHITE
    }
    override val node = object: HBox(avatar.root, label) {
        override fun getBaselineOffset(): Double = height * 0.75
        init {
            alignment = Pos.CENTER
            background = orangeBackground
        }
    }
    init {
        var server: Server? = null
        HBox.setMargin(avatar.root, insets)
        HBox.setMargin(label, insets)
        channel.consumeAsFlow().onEach {
            server = it.second
            avatar.updateUrl(null, it.second)
        }.flatMapLatest {
            userDatas.getAvatarUrlUpdates(it.first)
        }.onEach { url->
            debugAssert(server != null)
            server?.let {avatar.updateUrl(url, it)}
        }.launchIn(scope)
    }
    fun update(name: String, userId: UserId, server: Server) {
        debugAssertUiThread()
        label.text = name
        channel.offer(userId to server)
    }
    companion object {
        private val insets = Insets(0.0, 2.0, 0.0, 2.0)
        private val orangeBackground = Background(BackgroundFill(Color.DARKORANGE, CornerRadii(4.0), Insets.EMPTY))
        private val whiteBackground = Background(BackgroundFill(Color.WHITE, CornerRadii(2.0), Insets.EMPTY))
    }
}


fun messageSliceView(slice: TextSegment, server: Server,
                     data: UserDataStore
): FlowElement {
    return when(slice) {
        is PlainTextSegment -> InlineElement(Text(slice.text))
        is LinkTextSegment -> WebContentNode(slice.text, server)
        is EmojiTextSegment -> {
            val icon = EmojiIcon(slice.emoji, settings.fontSize)
            InlineElement(icon.node)
        }
        is UserIdLink -> {
            val link = UserLinkElement(data)
            link.update(slice.text, slice.userId, server)
            link
        }
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
        val url = link.toHttpUrlOrNull()
        url ?: return null
        val site = url.host
        val sview = siteViewConstructors.get(site)?.let { view -> view(url) }

        val filename = url.pathSegments.last()
        val ext = filename.substringAfter('.')
        val view = sview ?: mediaViewers.get(ext, url)
        return view
    }

    private fun setUpPrevie(view: ViewNode) {
        node.border = Border(BorderStroke(
                Color.LIGHTGRAY,
                BorderStrokeStyle.SOLID,
                CornerRadii(3.0, false),
                BorderWidths(1.0)
        ))
        node.background = Background(BackgroundFill(
                Color.TRANSPARENT,
                CornerRadii(3.0, false),
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

