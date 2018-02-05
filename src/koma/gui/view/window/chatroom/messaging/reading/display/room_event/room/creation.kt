package koma.gui.view.window.chatroom.messaging.reading.display.room_event.room

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.StackPane
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.showDatetime
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.showUser
import koma.matrix.event.room_message.state.MRoomCreate
import tornadofx.*

fun renderRoomCreation(message: MRoomCreate): Node {
    val _node = StackPane()
    _node.apply {
        hbox(spacing = 5.0) {
            alignment = Pos.CENTER
            text("This room is create by") {
                opacity = 0.5
            }
            showUser(this, message.sender)
        }
        showDatetime(this, message.origin_server_ts)
    }
    return _node
}
