package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message

import javafx.geometry.Insets
import javafx.scene.control.MenuItem
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import koma.Server
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content.*
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.DatatimeView
import koma.koma_app.AppStore
import koma.matrix.NotificationResponse
import koma.matrix.UserId
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.chat.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.gui.message.MessageCellContent
import link.continuum.desktop.observable.MutableObservable
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

@ExperimentalCoroutinesApi
class MRoomMessageViewNode(
   private val store: AppStore
): MessageCellContent<MRoomMessage> {
    private val scope = MainScope()
    override val root = StackPane()
    private val userData = store.userData
    private var item: MRoomMessage? = null
    private val timeView = DatatimeView()
    private val avatarView = AvatarView(userData = userData)
    private val senderLabel = Text()
    private val senderId = MutableObservable<UserId>()
    private val contentBox = HBox(5.0)
    private var content: ViewNode? = null

    companion object {
        private val pad2 = Insets(2.0)
    }
    init {
        with(root) {
            padding = pad2
            hbox(4.0) {
                minWidth = 1.0
                prefWidth = 1.0
                background = whiteBackGround
                padding = pad2
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
        senderId.flow().flatMapLatest {
            store.userData.getNameUpdates(it)
        }.onEach {
            senderLabel.text = it
        }.launchIn(scope)
    }

    override fun update(message: MRoomMessage, server: Server) {
        item = message
        releaseContentNode()
        senderId.set(message.sender)
        timeView.updateTime(message.origin_server_ts)
        avatarView.updateUser(message.sender, server)
        senderLabel.fill = userData.getUserColor(message.sender)
        val c = getContentNode(message, server)
        content = c
        contentBox.children.apply {
            clear()
            add(c.node)
        }
    }

    fun update(message: NotificationResponse.Event, server: Server, msg: M_Message) {
        item = null
        releaseContentNode()
        senderId.set(message.sender)
        timeView.updateTime(message.origin_server_ts)
        avatarView.updateUser(message.sender, server)
        senderLabel.fill = userData.getUserColor(message.sender)
        val c = getContentNode(msg, server, message.sender)
        content = c
        contentBox.children.apply {
            clear()
            add(c.node)
        }
    }
    private val copyTextMenuItem = MenuItem("Copy text").apply {
        action { copyText() }
    }
    override fun menuItems(): List<MenuItem> {
        return (content?.menuItems?: listOf()) + copyTextMenuItem
    }

    fun copyText() {
        item?.content?.body?.let {
            clipboardPutString(it)
        }
    }

    private fun getContentNode(message: MRoomMessage, server: Server): ViewNode {
        val content = message.content
        val sender = message.sender
        return getContentNode(content, server, sender)
    }
    private fun getContentNode(content: M_Message?, server: Server, sender: UserId): ViewNode {
        return when(content) {
            is TextMessage -> store.uiPools.msgText.take().apply {
                update(content, server)
            }
            is NoticeMessage -> store.uiPools.msgNotice.take().apply{
                update(content, server)
            }
            is EmoteMessage -> store.uiPools.msgEmote.take().apply{
                update(content, sender, server)
            }
            is ImageMessage -> store.uiPools.msgImage.take().apply{ update(content, server) }
            is FileMessage -> MFileViewNode(content, server)
            else ->store.uiPools.msgText.take().apply {
                updatePlainText(content?.body.toString(), server)
            }
        }
    }
    private fun releaseContentNode() {
        val c = content?: return
        when (c) {
            is MEmoteViewNode -> store.uiPools.msgEmote.pushBack(c)
            is MNoticeViewNode -> store.uiPools.msgNotice.pushBack(c)
            is MTextViewNode -> store.uiPools.msgText.pushBack(c)
            is MImageViewNode -> store.uiPools.msgImage.pushBack(c)
            else -> error("Unexpected node $c")
        }
    }

    override fun toString(): String {
        val body=item?.content?.body
        return "MessageCell(time=${timeView.root}, body=$body)"
    }
}

