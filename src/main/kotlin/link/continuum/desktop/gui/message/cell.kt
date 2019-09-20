package link.continuum.desktop.gui.message


import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import koma.Koma
import koma.gui.view.window.chatroom.messaging.reading.display.EventSourceViewer
import koma.gui.view.window.chatroom.messaging.reading.display.GuestAccessUpdateView
import koma.gui.view.window.chatroom.messaging.reading.display.HistoryVisibilityEventView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.MRoomMessageViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.member.MRoomMemberViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.room.MRoomCreationViewNode
import koma.koma_app.AppStore
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.state.MRoomCreate
import koma.matrix.event.room_message.state.MRoomGuestAccess
import koma.matrix.event.room_message.state.MRoomHistoryVisibility
import koma.matrix.event.room_message.state.MRoomMember
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.getEvent
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.hbox
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.gui.showIf
import model.Room
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private val sourceViewer by lazy { EventSourceViewer() }

fun createCell(item: RoomEvent?, km: Koma, store: AppStore): MessageCell {
    val x = when (item) {
        is MRoomMember -> MRoomMemberViewNode(store)
        is MRoomMessage -> MRoomMessageViewNode(km, store)
        is MRoomCreate -> MRoomCreationViewNode(store)
        is MRoomGuestAccess -> GuestAccessUpdateView(store)
        is MRoomHistoryVisibility -> HistoryVisibilityEventView(store)
        else -> FallbackCell(store)
    }
    return x
}

class FallbackCell(store: AppStore):MessageCell(store){
    private val text= Text()
    private var msg: RoomEventRow? = null
    override val center = HBox(5.0).apply {
        alignment = Pos.CENTER
        add(text)
        add(Hyperlink().apply {
            setOnAction {
                msg?.let {
                    sourceViewer.showAndWait(it)
                }
            }
        })
    }
    init {
        node.add(center)
    }
    override fun updateItem(item: Pair<RoomEventRow, Room>?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
        } else {
            updateEvent(item.first, item.second)
            text.text= item.first.getEvent()?.type.toString()
            msg = item.first
            graphic = node
        }
    }
}

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
abstract class MessageCell(
        protected val store: AppStore
): ListCell<Pair<RoomEventRow, Room>>() {
    protected abstract val center: Region
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
    protected val menu: ContextMenu = ContextMenu().apply {
        items.add(contextMenuShowSource)
    }
    protected var current: RoomEventRow? = null

    // share between different types of view
    protected val senderAvatar = AvatarView(store.userData)

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
            menu.show(node, event.screenX, event.screenY)
            event.consume()
        }
    }
}
