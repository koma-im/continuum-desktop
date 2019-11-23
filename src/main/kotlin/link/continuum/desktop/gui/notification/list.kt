package link.continuum.desktop.gui.notification

import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.util.Callback
import koma.Server
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content.MessageView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.DatatimeView
import koma.koma_app.AppStore
import koma.matrix.NotificationResponse
import koma.matrix.UserId
import koma.matrix.event.room_message.RoomEventType
import koma.matrix.json.MoshiInstance
import koma.util.getOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.util.Account
import mu.KotlinLogging

private typealias Notification = NotificationResponse.Notification
private typealias Item = Notification
private typealias List = ListView<Item>
private typealias NotificationEvent = NotificationResponse.Event<NotificationResponse.Content>

private val logger = KotlinLogging.logger {}

class NotificationList(
        account: Account,
        store: AppStore
): CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val context = ListContext(null)
    private val placeholderText = Text("Loading notifications")
    val root = List().apply{
        placeholder = HBox().apply {
            alignment = Pos.CENTER
            vbox {
                alignment = Pos.CENTER
                children.add(TextFlow(placeholderText))
            }
        }
        cellFactory = Callback<List, ListCell<Item>> {
            NotificationCell(store, context)
        }
    }
    fun updateServer(server: Server) {
        context.server = server
    }
    init {
        launch(Dispatchers.Main) {
            val r = account.getNotifications(limit=20)
            if (r.isFailure) {
                placeholderText.text = "Couldn't load notifications: ${r.failureOrNull()}"
                logger.error { "error $r"}
                return@launch
            }
            placeholderText.text = "No notifications yet"
            val notifications = r.getOrThrow()
            root.items.setAll(notifications.notifications.sortedBy { it.ts })
        }
    }
}

private class ListContext(
        var server: Server?
)

private class NotificationCell(
        private val store: AppStore,
        private val context: ListContext
): ListCell<Item>(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val timeView = DatatimeView()
    private val avatarView = AvatarView(userData = store.userData)
    private val senderLabel = Text()
    private val center = StackPane()
    private var server: Server? = null
    private var item: Item? = null
    private val menu by lazy {
        ContextMenu(Menu("Debug", null,
                MenuItem("Show popup").apply {
                    action { showPopup() }
                }
        ))
    }
    val root = HBox(4.0).apply {
        minWidth = 1.0
        prefWidth = 1.0
        background = whiteBackGround
        alignment = Pos.TOP_LEFT
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
        setOnContextMenuRequested {
            menu.show(this, null, it.x, it.y)
        }
    }
    private val messageView = MessageView(store.userData)
    private val fallbackCell = TextFlow().apply {
        // wrap text
        minWidth = 1.0
        prefWidth = 1.0
    }

    private val senderId = Channel<UserId>(Channel.CONFLATED)

    private fun showPopup() {
        val s = server ?: return
        val item = this.item ?: return
        popNotify(item, store, s)
    }
    override fun updateItem(item: Notification?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
            return
        }
        val event = item.event
        val content = event.content
        val s = context.server ?: run {
            graphic = TextFlow(Text("type: ${event.type}, content: ${event.content}"))
            logger.error { "no server"}
            return
        }
        this.server = s
        this.item = item
        center.children.clear()
        senderId.offer(event.sender)
        timeView.updateTime(event.origin_server_ts)
        avatarView.updateUser(event.sender, s)
        senderLabel.fill = store.userData.getUserColor(event.sender)
        when {
            event.type == RoomEventType.Message -> if(content is NotificationResponse.Content.Message) {
                val msg = content.message
                messageView.update(msg, s, event.sender)
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
}