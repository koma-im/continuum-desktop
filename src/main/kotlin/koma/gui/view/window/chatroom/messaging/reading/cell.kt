package koma.gui.view.window.chatroom.messaging.reading

import javafx.scene.control.ListCell
import koma.gui.view.window.chatroom.messaging.reading.display.MessageCell
import koma.storage.message.MessageManager
import link.continuum.database.models.RoomEventRow
import link.continuum.desktop.gui.list.user.UserDataStore
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

class RoomEventCell(private val server: HttpUrl,
                    private val manager: MessageManager,
                    store: UserDataStore,
                    client: OkHttpClient) : ListCell<RoomEventRow>() {
    val cell = MessageCell(server, manager, store,  client)
    override fun updateItem(item: RoomEventRow?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
        } else {
            cell.updateEvent(item)
            graphic = cell.node
        }
    }
}

