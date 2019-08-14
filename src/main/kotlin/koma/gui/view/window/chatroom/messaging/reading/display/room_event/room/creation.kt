package koma.gui.view.window.chatroom.messaging.reading.display.room_event.room

import javafx.geometry.Pos
import javafx.scene.control.MenuItem
import javafx.scene.layout.StackPane
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.DatatimeView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.StateEventUserView
import koma.koma_app.AppStore
import koma.koma_app.appState
import koma.matrix.event.room_message.state.MRoomCreate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.database.models.RoomEventRow
import link.continuum.database.models.getEvent
import link.continuum.desktop.gui.list.user.UserDataStore
import link.continuum.desktop.gui.message.MessageCell
import link.continuum.desktop.util.http.MediaServer
import model.Room
import tornadofx.*

@ExperimentalCoroutinesApi
class MRoomCreationViewNode constructor(
        store: AppStore
): MessageCell(store) {
    override val center = StackPane()

    private val userView = StateEventUserView(store.userData, store.settings.scaling * 32.0)
    private val timeView = DatatimeView()


    init {
        node.apply {
            hbox(spacing = 5.0) {
                alignment = Pos.CENTER
                text("This room is create by") {
                    opacity = 0.5
                }
                add(userView.root)

            }
            add(timeView.root)
        }
    }

    override fun updateItem(item: Pair<RoomEventRow, Room>?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
        } else {
            val ev = item.first.getEvent()
            if (ev !is MRoomCreate) {
                graphic = null
            } else {
                updateEvent(item.first, item.second)
                update(ev, item.second.account.server)
                graphic = node
            }
        }
    }
    fun update(message: MRoomCreate, mediaServer: MediaServer) {
        userView.updateUser(message.sender, mediaServer)
        timeView.updateTime(message.origin_server_ts)
    }
}
