package koma.gui.view.window.chatroom.messaging.reading.display

import javafx.scene.Node
import javafx.scene.layout.Region
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.renderMessageFromUser
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.member.renderMemberChange
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.room.renderRoomCreation
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.RoomEvent
import koma.matrix.event.room_message.state.MRoomCreate
import koma.matrix.event.room_message.state.MRoomMember
import org.fxmisc.flowless.Cell

class MessageCell(val message: RoomEvent): Cell<RoomEvent, Node> {
    private val _node: Node

    init {
        _node = when(message) {
            is MRoomMember -> renderMemberChange(message)
            is MRoomCreate -> renderRoomCreation(message)
            is MRoomMessage -> renderMessageFromUser(message)
            else -> Region()
        }
    }

    override fun getNode(): Node {
        return _node
    }
}

