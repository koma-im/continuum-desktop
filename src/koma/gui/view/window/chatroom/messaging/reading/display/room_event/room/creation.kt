package koma.gui.view.window.chatroom.messaging.reading.display.room_event.room

import javafx.geometry.Pos
import javafx.scene.control.MenuItem
import javafx.scene.layout.StackPane
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.showDatetime
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.showUser
import koma.matrix.event.room_message.state.MRoomCreate
import tornadofx.*

class MRoomCreationViewNode(message: MRoomCreate): ViewNode {
    override val node = StackPane()
    override val menuItems: List<MenuItem>
        get() = listOf()

    init {
        node.apply {
            hbox(spacing = 5.0) {
                alignment = Pos.CENTER
                text("This room is create by") {
                    opacity = 0.5
                }
                showUser(this, message.sender)
            }
            showDatetime(this, message.origin_server_ts)
        }
    }
}
