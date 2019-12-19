package link.continuum.desktop.gui.message


import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.MenuItem
import javafx.scene.layout.Region
import javafx.scene.text.Text
import koma.gui.view.window.chatroom.messaging.reading.display.EventSourceViewer
import koma.gui.view.window.chatroom.messaging.reading.display.GuestAccessUpdateView
import koma.gui.view.window.chatroom.messaging.reading.display.HistoryVisibilityEventView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.MRoomMessageViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.member.MRoomMemberViewNode
import koma.koma_app.AppStore
import koma.matrix.event.room_message.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.getEvent
import link.continuum.desktop.Room
import link.continuum.desktop.gui.*
import link.continuum.desktop.util.http.MediaServer
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private val sourceViewer by lazy { EventSourceViewer() }

class FallbackCell():MessageCellContent<RoomEvent> {
    private val text= Text()
    private var msg: RoomEventRow? = null
    override val root = HBox(5.0).apply {
        alignment = Pos.CENTER
        add(text)
    }

    override fun update(message: RoomEvent, server: MediaServer) {
        text.text= message.type.toString()
    }
}

interface MessageCellContent<T> {
    val root: Node
    fun menuItems(): List<MenuItem> = listOf()
    fun update(message: T, server: MediaServer)
}

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
open class MessageCell(
        protected val store: AppStore
): ListCell<Pair<RoomEventRow, Room>>() {
    protected open val center: Region = StackPane()
    private val loading = Label("Loading older messages...")

    val node = VBox(3.0).apply {
        hbox {
            alignment = Pos.CENTER
            add(loading)
        }
    }
    protected val contextMenuShowSource = MenuItem("View Source").apply {
        setOnAction {
            current?.let {
                sourceViewer.showAndWait(it)
            }
        }
    }
    protected val menu: ContextMenu = ContextMenu()
    protected var current: RoomEventRow? = null
    private var currentContent: Any? = null

    override fun updateItem(item: Pair<RoomEventRow, Room>?, empty: Boolean) {
        super.updateItem(item, empty)
        releaseContentCell()
        if (empty || item == null) {
            graphic = null
            return
        }
        val event = item.first.getEvent() ?: return
        val server = item.second.account.server
        val content = when (event) {
            is MRoomMessage -> store.messageCells.take().apply {
                update(event, server)
            }
            is MRoomMember -> store.membershipCells.take().apply {
                update(event, server)
            }
            is MRoomGuestAccess -> store.guestAccessCells.take().apply {
                update(event, server)
            }
            is MRoomHistoryVisibility -> store.historyVisibilityCells.take().apply {
                update(event, server)
            }
            else -> store.fallbackCells.take().apply {
                update(event, server)
            }
        }
        currentContent = content
        node.children.run {
            if (size > 1) set(1, content.root)
            else add(content.root)
            Unit
        }
        updateEvent(item.first, item.second)
        graphic = node
    }
    private fun releaseContentCell() {
        val c = currentContent?: return
        currentContent = null
        when (c) {
            is MRoomMessageViewNode -> store.messageCells.pushBack(c)
            is MRoomMemberViewNode -> store.membershipCells.pushBack(c)
            is GuestAccessUpdateView -> store.guestAccessCells.pushBack(c)
            is HistoryVisibilityEventView -> store.historyVisibilityCells.pushBack(c)
            is FallbackCell -> store.fallbackCells.pushBack(c)
            else -> error("Unexpected node $c")
        }
    }
    protected fun updateEvent(message: RoomEventRow, room: Room) {
        loading.managedProperty().unbind()
        loading.visibleProperty().unbind()
        loading.showIf(false)
        if(!message.preceding_stored) {
            logger.trace { "messages before ${message.event_id} are not stored yet" }
            loading.showIf(true)
            val status = room.messageManager.fetchPrecedingRows(message)
            loading.managedProperty().bind(status)
            loading.visibleProperty().bind(status)
        }
        current = message
    }
    init {
        node.setOnContextMenuRequested { event ->
            val c = currentContent
            if (c is MessageCellContent<*>) {
                menu.items.run {
                    clear()
                    addAll(c.menuItems())
                    add(contextMenuShowSource)
                }
            }
            menu.show(node, event.screenX, event.screenY)
            event.consume()
        }
    }
}
