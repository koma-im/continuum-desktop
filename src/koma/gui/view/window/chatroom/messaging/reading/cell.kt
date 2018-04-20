package koma.gui.view.window.chatroom.messaging.reading

import javafx.scene.control.ListCell
import koma.gui.view.window.chatroom.messaging.reading.display.MessageCell
import koma.matrix.event.room_message.RoomEvent

class RoomEventCell : ListCell<RoomEvent>() {

    override fun updateItem(item: RoomEvent?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item != null) {
            val c = MessageCell(item)
            graphic = c.node
        } else {
        }
    }
}

