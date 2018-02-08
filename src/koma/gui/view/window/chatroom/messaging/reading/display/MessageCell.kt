package koma.gui.view.window.chatroom.messaging.reading.display

import javafx.scene.Node
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.MenuItem
import javafx.scene.control.TextArea
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.MRoomMessageViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.member.MRoomMemberViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.room.MRoomCreationViewNode
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.state.MRoomCreate
import koma.matrix.event.room_message.state.MRoomMember
import org.fxmisc.flowless.Cell
import tornadofx.*

class MessageCell(val message: RoomEvent): Cell<RoomEvent, Node> {
    private val _node: Node

    init {
        val vn = when(message) {
            is MRoomMember -> MRoomMemberViewNode(message)
            is MRoomCreate -> MRoomCreationViewNode(message)
            is MRoomMessage -> MRoomMessageViewNode(message)
            else -> null
        }
        if (vn == null)
            _node = Region()
        else {
            _node = vn.node
            _node.contextmenu {
                this.items.addAll(vn.menuItems)
                item("View Source").action { showSource(roomEvent = message) }
            }
        }
    }

    override fun getNode(): Node {
        return _node
    }
}

interface ViewNode {
    val node: Region
    val menuItems: List<MenuItem>
}

fun showSource(roomEvent: RoomEvent) {
    val src = roomEvent.toJson(indent = true)

    val dialog = Dialog<Unit>()
    dialog.title = "Room Event Source"

    val head = Text("Room Event Source")
    val textArea = TextArea(src)
    textArea.isEditable = false
    textArea.hgrow = Priority.ALWAYS
    val content = VBox(head, textArea)
    dialog.dialogPane.content = content

    dialog.dialogPane.buttonTypes.add(ButtonType.CLOSE)
    dialog.showAndWait()
}
