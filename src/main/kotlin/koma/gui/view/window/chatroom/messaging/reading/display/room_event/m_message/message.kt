package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.MenuItem
import javafx.scene.input.Clipboard
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import koma.Koma
import koma.Server
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content.MessageView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.DatatimeView
import koma.koma_app.AppStore
import koma.koma_app.appState
import koma.matrix.UserId
import koma.matrix.event.room_message.MRoomMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.getEvent
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.gui.message.MessageCell
import model.Room
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class MRoomMessageViewNode(
        private val km: Koma,
        store: AppStore
): MessageCell(store) {
    override val center = StackPane()
    init {
        node.add(center)
    }
    private val userData = store.userData
    private var item: MRoomMessage? = null
    private val timeView = DatatimeView()
    private val avatarView = AvatarView(userData = userData)
    private val senderLabel = Text()
    private val senderId = Channel<UserId>(Channel.CONFLATED)
    private val contentBox = HBox(5.0)
    private val content by  lazy { MessageView(userData, km) }

    companion object {
        private val pad2 = Insets(2.0)
    }
    init {
        with(center) {
            padding = pad2
            hbox(4.0) {
                minWidth = 1.0
                prefWidth = 1.0
                background = whiteBackGround
                padding = pad2
                alignment = Pos.CENTER_LEFT
                add(avatarView.root)

                vbox {
                    spacing = 2.0
                    HBox.setHgrow(this, Priority.ALWAYS)
                    hbox(spacing = 10.0) {
                        HBox.setHgrow(this, Priority.ALWAYS)
                        add(senderLabel)
                        add(timeView.root)
                    }
                    add(contentBox)
                }
            }
        }
        GlobalScope.launch {
            val newName = switchUpdates(senderId) { userData.getNameUpdates(it) }
            for (n in newName) {
                withContext(UiDispatcher) {
                    senderLabel.text = n
                }
            }
        }
    }

    override fun updateItem(item: Pair<RoomEventRow, Room>?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
            text = null
        } else {
            val ev = item.first.getEvent()
            if (ev !is MRoomMessage) {
                graphic = null
                text = "ev $ev"
            } else {
                text = null
                updateEvent(item.first, item.second)
                update(ev, item.second.account.server)
                graphic = node
            }
        }
    }
    fun update(message: MRoomMessage, server: Server) {
        item = message
        senderId.offer(message.sender)
        timeView.updateTime(message.origin_server_ts)
        avatarView.updateUser(message.sender, server)
        senderLabel.fill = userData.getUserColor(message.sender)
        content.update(message, server)
        contentBox.children.apply {
            clear()
            content.node?.node?.let { this.add(it) }
        }

        menu.items.apply {
            clear()
            content.node?.menuItems?.let { addAll(it) }
            add(MenuItem("Copy text").apply {
                action { copyText() }
            })
            add(contextMenuShowSource)
        }
    }

    fun copyText() {
        item?.content?.body?.let {
            clipboardPutString(it)
        }
    }

    override fun toString(): String {
        val body=item?.content?.body
        return "MessageCell index=$index, time=${timeView.text.text}, body=$body"
    }
}

