package koma.gui.view.window.chatroom.messaging.reading.display

import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.MRoomMessageViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.member.MRoomMemberViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.room.MRoomCreationViewNode
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.state.MRoomCreate
import koma.matrix.event.room_message.state.MRoomMember
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
            showSource(roomEvent = it)
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
                memberView.update(ev)
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

fun showSource(roomEvent: RoomEventRow) {
    val src = roomEvent.json
    val fmt = formatJson(src)
    val dialog = Dialog<Unit>()
    dialog.title = "Room Event Source"

    val head = Text("Room Event Source")
    val textArea = TextArea(fmt)
    textArea.isEditable = false
    textArea.hgrow = Priority.ALWAYS
    val content = VBox(head, textArea)
    dialog.dialogPane.content = content

    dialog.dialogPane.buttonTypes.add(ButtonType.CLOSE)
    dialog.showAndWait()
}
