package koma.gui.view.window.chatroom.messaging.reading

import javafx.scene.control.ListCell
import koma.Koma
import koma.Server
import koma.gui.view.window.chatroom.messaging.reading.display.MessageCell
import koma.storage.message.MessageManager
import link.continuum.database.models.RoomEventRow
import link.continuum.desktop.gui.list.user.UserDataStore
import model.Room
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

class RoomEventCell(koma: Koma,
                    store: UserDataStore
) : ListCell<Pair<RoomEventRow, Room>>() {
    val cell = MessageCell(koma, store)
    override fun updateItem(item: Pair<RoomEventRow, Room>?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
        } else {
            cell.updateEvent(item.first, item.second)
            graphic = cell.node
        }
    }
}

