package koma.gui.view.window.chatroom.messaging.reading.display

import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.MRoomMessageViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.member.MRoomMemberViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.room.MRoomCreationViewNode
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.state.MRoomCreate
import koma.matrix.event.room_message.state.MRoomMember
import koma.matrix.json.MoshiInstance
import koma.util.formatJson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.database.models.RoomEventRow
import link.continuum.desktop.database.models.getEvent
import link.continuum.desktop.gui.list.user.UserDataStore
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import tornadofx.*

@ExperimentalCoroutinesApi
class MessageCell(
        private val server: HttpUrl,
        store: UserDataStore,
        client: OkHttpClient
) {
    val node = StackPane()
    private val contextMenu: ContextMenu
    private val contextMenuShowSource = MenuItem("View Source").apply {
        action { current?.let {
            sourceViewer.showAndWait(it)
        }
        }
    }
    private var current: RoomEventRow? = null

    private val memberView = MRoomMemberViewNode(store, client)

    fun updateEvent(message: RoomEventRow) {
        current = message
        node.children.clear()
        contextMenu.items.clear()
        val ev = message.getEvent()
        val vn = when(ev) {
            is MRoomMember -> {
                memberView.update(ev, server)
                memberView
            }
            is MRoomCreate -> MRoomCreationViewNode(ev)
            is MRoomMessage -> MRoomMessageViewNode(ev, server)
            else -> null
        }
        if (vn!= null) {
            node.children.add(vn.node)
            contextMenu.items.addAll(vn.menuItems)
            contextMenu.items.add(contextMenuShowSource)
        }
    }
    init {
        contextMenu = node.contextmenu()
    }
}

fun RoomEvent.supportedByDisplay(): Boolean
        = when (this) {
    is MRoomMember,
    is MRoomCreate,
    is MRoomMessage -> true
    else -> false
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
