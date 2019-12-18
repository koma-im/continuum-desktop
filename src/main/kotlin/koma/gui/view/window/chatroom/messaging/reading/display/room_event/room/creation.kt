package koma.gui.view.window.chatroom.messaging.reading.display.room_event.room

import javafx.geometry.Pos
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.DatatimeView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.StateEventUserView
import koma.koma_app.AppStore
import koma.matrix.event.room_message.MRoomCreate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.HBox
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.message.MessageCellContent
import link.continuum.desktop.gui.text
import link.continuum.desktop.util.http.MediaServer

@ExperimentalCoroutinesApi
class MRoomCreationViewNode constructor(
        store: AppStore
): MessageCellContent<MRoomCreate> {
    override val root = HBox(5.0)

    private val userView = StateEventUserView(store.userData)
    private val timeView = DatatimeView()

    init {
        root.apply {
            alignment = Pos.CENTER
            text("This room is create by") {
                opacity = 0.5
            }
            add(userView.root)
            add(timeView.root)
        }
    }
    override fun update(message: MRoomCreate, mediaServer: MediaServer) {
        userView.updateUser(message.sender, mediaServer)
        timeView.updateTime(message.origin_server_ts)
    }
}
