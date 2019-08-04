package koma.gui.view.window.chatroom.messaging.reading.display

import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.text.Text
import koma.Koma
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.MRoomMessageViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.member.MRoomMemberViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.room.MRoomCreationViewNode
import koma.koma_app.appState
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.state.MRoomCreate
import koma.matrix.event.room_message.state.MRoomGuestAccess
import koma.matrix.event.room_message.state.MRoomHistoryVisibility
import koma.matrix.event.room_message.state.MRoomMember
import koma.matrix.json.MoshiInstance
import koma.matrix.room.visibility.HistoryVisibility
import koma.util.formatJson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.getEvent
import link.continuum.desktop.gui.icon.avatar.AvatarView
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.gui.showIf
import model.Room
import mu.KotlinLogging
import tornadofx.*

private val logger = KotlinLogging.logger {}

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class MessageCell(
        private val km: Koma,
        store: UserDataStore
) {
    private val center = StackPane()
    private val loading = Label("Loading older messages...")

    val node = VBox(3.0).apply {
        hbox {
            alignment = Pos.CENTER
            add(loading)
        }
        add(center)
    }
    private val contextMenu: ContextMenu
    private val contextMenuShowSource = MenuItem("View Source").apply {
        action { current?.let {
            sourceViewer.showAndWait(it)
        }
        }
    }
    private var current: RoomEventRow? = null

    private val avSize = appState.store.settings.scaling * 32.0
    // share between different types of view
    private val senderAvatar = AvatarView(store, avSize)
    private val memberView = MRoomMemberViewNode(store)
    private val messageView by lazy { MRoomMessageViewNode(km,  store) }
    private val creationView by lazy { MRoomCreationViewNode(store, avSize ) }
    private val historyVisibilityView by lazy { HistoryVisibilityEventView() }
    private val guestAccessUpdateView by lazy {GuestAccessUpdateView()}


    fun updateEvent(message: RoomEventRow, room: Room) {
        loading.managedProperty().unbind()
        loading.visibleProperty().unbind()
        loading.showIf(false)
        if(!message.preceding_stored) {
            logger.debug { "messages before ${message.event_id} are not stored yet" }
            loading.showIf(true)
            val status = room.messageManager.fetchPrecedingRows(message)
            loading.managedProperty().bind(status)
            loading.visibleProperty().bind(status)
        }
        current = message
        center.children.clear()
        contextMenu.items.clear()

        val ev = message.getEvent()
        val vn = when(ev) {
            is MRoomMember -> {
                memberView.update(ev, room.account.server)
                memberView
            }
            is MRoomCreate -> {
                creationView.update(ev, room.account.server)
                creationView
            }
            is MRoomMessage -> {
                messageView.update(ev, room.account.server)
                messageView
            }
            is MRoomHistoryVisibility -> {
                senderAvatar.updateUser(ev.sender, room.account.server)
                historyVisibilityView.apply { update(senderAvatar, ev) }
            }
            is MRoomGuestAccess -> {
                senderAvatar.updateUser(ev.sender, room.account.server)
                guestAccessUpdateView.update(senderAvatar, ev)
                guestAccessUpdateView
            }
            else -> {
                center.children.add(HBox(5.0).apply {
                    alignment = Pos.CENTER
                    text("${ev?.type} event")
                    hyperlink("View source").action {
                        sourceViewer.showAndWait(message)
                    }
                })
                null
            }
        }
        if (vn!= null) {
            center.children.add(vn.node)
            contextMenu.items.addAll(vn.menuItems)
        }
        contextMenu.items.add(contextMenuShowSource)
    }
    init {
        contextMenu = node.contextmenu()
    }
}

@ExperimentalCoroutinesApi
class HistoryVisibilityEventView(
): ViewNode {
    override val menuItems: List<MenuItem> = listOf()
    private val sender = HBox()
    private val text = Text()
    override val node = HBox(5.0).apply {
        alignment = Pos.CENTER
        children.addAll(sender, text)
    }

    fun update(senderAvatar: AvatarView, ev: MRoomHistoryVisibility) {
        sender.children.clear()
        sender.children.add(senderAvatar.root)
        val t = when(ev.content.history_visibility) {
            HistoryVisibility.Invited -> "made events accessible to newly joined members " +
                    "from the point they were invited onwards"
            HistoryVisibility.Joined -> "made events accessible to newly joined members " +
                    "from the point they joined the room onwards"
            HistoryVisibility.Shared -> "made future room history visible to all members"
            HistoryVisibility.WorldReadable -> "made new events visible to the world"
        }
        text.text = t
    }
}

@ExperimentalCoroutinesApi
class GuestAccessUpdateView(): ViewNode {
    override val node = HBox(5.0).apply {
        alignment = Pos.CENTER
    }
    override val menuItems: List<MenuItem>
        get() = listOf()
    fun update(senderAvatar: AvatarView, event: MRoomGuestAccess) {
        node.children.clear()
        node.children.addAll(senderAvatar.root,
                Text("set guest access to ${event.content.guest_access}")
                )
    }
}

interface ViewNode {
    val node: Region
    val menuItems: List<MenuItem>
}

private val sourceViewer by lazy { EventSourceViewer() }

class EventSourceViewer{
    private val dialog = Dialog<Unit>()
    private val textArea = TextArea()
    private var raw: String = ""
    private var processed: String = ""
    fun showAndWait(roomEvent: RoomEventRow) {
        raw = formatJson(roomEvent.json)
        processed = formatJson(MoshiInstance.roomEventAdapter.toJson(roomEvent.getEvent()))
        textArea.text = raw
        dialog.showAndWait()
    }

    init {
        textArea.isEditable = false
        textArea.hgrow = Priority.ALWAYS
        textArea.vgrow = Priority.ALWAYS
        val head = HBox().apply {
            vbox {
                text("Room Event Source")
                alignment = Pos.CENTER_LEFT
                hgrow = Priority.ALWAYS
            }
            buttonbar {
                button("Raw") {
                    tooltip = Tooltip("Json string from server")
                    setOnAction {
                        textArea.text = raw
                    }
                }
                button("Processed") {
                    tooltip = Tooltip("Portion of json that is supported")
                    setOnAction {
                        textArea.text = processed
                    }
                }
            }
        }
        dialog.apply {
            title = "Room Event Source"
            isResizable = true
            dialogPane.apply {
                content = VBox(5.0, head, textArea).apply {
                    vgrow = Priority.ALWAYS
                }
                buttonTypes.add(ButtonType.CLOSE)
            }
        }
    }
}
