package koma.gui.view.window.chatroom.messaging.reading

import javafx.scene.control.ListCell
import koma.gui.view.window.chatroom.messaging.reading.display.MessageCell
import link.continuum.desktop.database.models.RoomEventRow
import okhttp3.HttpUrl

class RoomEventCell(private val server: HttpUrl) : ListCell<RoomEventRow>() {

    override fun updateItem(item: RoomEventRow?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item != null) {
            val c = MessageCell(item, server)
            graphic = c.node
        } else {
        }
    }
}

