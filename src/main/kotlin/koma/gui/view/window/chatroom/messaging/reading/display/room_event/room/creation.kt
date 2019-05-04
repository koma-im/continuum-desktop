package koma.gui.view.window.chatroom.messaging.reading.display.room_event.room

import javafx.geometry.Pos
import javafx.scene.control.MenuItem
import javafx.scene.layout.StackPane
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.DatatimeView
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.util.StateEventUserView
import koma.koma_app.appState
import koma.matrix.event.room_message.state.MRoomCreate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import link.continuum.desktop.gui.list.user.UserDataStore
import okhttp3.OkHttpClient
import tornadofx.*

@ExperimentalCoroutinesApi
class MRoomCreationViewNode constructor(
        store: UserDataStore,
        client: OkHttpClient,
        avatarsize: Double = appState.store.settings.scaling * 32.0
): ViewNode {
    override val node = StackPane()
    override val menuItems: List<MenuItem>
        get() = listOf()

    private val userView = StateEventUserView(store, client, avatarsize)
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

    fun update(message: MRoomCreate) {
        userView.updateUser(message.sender)
        timeView.updateTime(message.origin_server_ts)
    }
}
