package link.continuum.desktop.gui.notification

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content.MessageView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.DatatimeView
import koma.koma_app.AppStore
import koma.matrix.NotificationResponse
import koma.matrix.UserId
import koma.matrix.event.room_message.RoomEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.util.http.MediaServer
import mu.KotlinLogging
import org.controlsfx.control.Notifications

private typealias NotificationData = NotificationResponse.Notification
private val logger = KotlinLogging.logger {}

fun popNotify(notification: NotificationData, store: AppStore, server: MediaServer) {
    val popup = Notifications.create()
            .title("Incoming message")
            .position(Pos.TOP_RIGHT)
    val graphic = NotificationGraphic(store)
    graphic.updateItem(notification, server)
    popup.graphic(graphic.graphic)
    popup.show()
}


private class NotificationGraphic(
        private val store: AppStore
): CoroutineScope by CoroutineScope(Dispatchers.Default) {
    var graphic: Node? = null
        get() = field
        private set(value) {field = value}
    private val timeView = DatatimeView()
    private val avatarView = AvatarView(userData = store.userData)
    private val senderLabel = Text()
    private val center = StackPane()
    val root = object : HBox(4.0) {
        init {
            add(avatarView.root)

            vbox {
                spacing = 2.0
                HBox.setHgrow(this, Priority.ALWAYS)
                hbox(spacing = 10.0) {
                    add(senderLabel)
                    add(timeView.root)
                }
                add(center)
            }
            style = boxStyle
        }

        override fun computeMinHeight(width: Double): Double {
            return super.computeMinHeight(width) + 60.0
        }
    }
    private val messageView = MessageView(store.userData)
    private val fallbackCell = TextFlow()

    private val senderId = Channel<UserId>(Channel.CONFLATED)

    fun updateItem(item: NotificationData?, server: MediaServer) {
        if (null == item) {
            graphic = null
            return
        }
        val event = item.event
        val content = event.content
        center.children.clear()
        senderId.offer(event.sender)
        timeView.updateTime(event.origin_server_ts)
        avatarView.updateUser(event.sender, server)
        senderLabel.fill = store.userData.getUserColor(event.sender)
        when {
            event.type == RoomEventType.Message -> if(content is NotificationResponse.Content.Message) {
                val msg = content.message
                messageView.update(msg, server, event.sender)
                messageView.node?.let {
                    center.children.add(it.node)
                }
            } else {
                logger.debug { "msg content $content"}
            }
            else -> {
                fallbackCell.children.setAll(Text("type: ${event.type}, content: ${event.content}"))
                center.children.add(fallbackCell)
            }
        }
        graphic = root
    }
    init {
        launch(Dispatchers.Main) {
            val newName = switchUpdates(senderId) { store.userData.getNameUpdates(it) }
            for (n in newName) {
                senderLabel.text = n
            }
        }
    }

    companion object {
        private val boxStyle = StyleBuilder {
            prefWidth = 12.em
        }.toStyle()
    }
}