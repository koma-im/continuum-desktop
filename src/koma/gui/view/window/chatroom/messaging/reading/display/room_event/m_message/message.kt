package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message

import javafx.geometry.Pos
import javafx.scene.control.MenuItem
import javafx.scene.input.Clipboard
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content.MessageView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.DatatimeView
import koma.koma_app.appState
import koma.matrix.UserId
import koma.matrix.event.room_message.MRoomMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.continuum.desktop.gui.UiDispatcher
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.gui.switchUpdates
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import tornadofx.*

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class MRoomMessageViewNode(
        server: HttpUrl,
        private val store: UserDataStore,
        client: OkHttpClient,
        avatarSize: Double = appState.store.settings.scaling * 32.0
): ViewNode {
    override val node = StackPane()
    override val menuItems = mutableListOf<MenuItem>()

    private var item: MRoomMessage? = null
    private val timeView = DatatimeView()
    private val avatarView = AvatarView(avatarSize = avatarSize, userData = store, client = client)
    private val senderLabel = Text()
    private val senderId = Channel<UserId>(Channel.CONFLATED)
    private val contentBox = HBox(5.0)
    private val content by  lazy { MessageView(store, server) }

    init {
        with(node) {
            paddingAll = 2.0
            hbox {
                minWidth = 1.0
                prefWidth = 1.0
                style {
                    alignment = Pos.CENTER_LEFT
                    paddingAll = 2.0
                    backgroundColor = multi(Color.WHITE)
                }
                add(avatarView.root)

                vbox(spacing = 2.0) {
                    hgrow = Priority.ALWAYS
                    hbox(spacing = 10.0) {
                        hgrow = Priority.ALWAYS
                        add(senderLabel)
                        add(timeView.root)
                    }
                    add(contentBox)
                }
            }
        }
        GlobalScope.launch {
            val newName = switchUpdates(senderId) { store.getNameUpdates(it) }
            for (n in newName) {
                withContext(UiDispatcher) {
                    senderLabel.text = n
                }
            }
        }
    }
    fun update(message: MRoomMessage) {
        item = message
        senderId.offer(message.sender)
        timeView.updateTime(message.origin_server_ts)
        avatarView.updateUser(message.sender)
        senderLabel.fill = store.getUserColor(message.sender)
        content.update(message)
        contentBox.children.apply {
            clear()
            content.node?.node?.let { this.add(it) }
        }

        menuItems.apply {
            clear()
            content.node?.menuItems?.let { addAll(it) }
            add(MenuItem("Copy text").apply {
                action { copyText() }
            })
        }
    }

    fun copyText() {
        item?.content?.body?.let {
            Clipboard.getSystemClipboard().putString(it)
        }
    }
}

