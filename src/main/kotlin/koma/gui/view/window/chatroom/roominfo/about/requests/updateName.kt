package koma.gui.view.window.chatroom.roominfo.about.requests

import koma.matrix.event.room_message.state.RoomNameContent
import koma.koma_app.appState
import koma.util.onFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.JFX
import model.Room
import org.controlsfx.control.Notifications
import tornadofx.*

fun requestUpdateRoomName(room: Room, input: String?) {
    input?:return
    val api = appState.apiClient
    api ?: return
    val name = RoomNameContent(input)
    GlobalScope.launch {
        val result = api.setRoomName(room.id, name)
        result.onFailure {
            val message = it.message
            launch(Dispatchers.JavaFx) {
                Notifications.create()
                        .title("Failed to set room name $name")
                        .text("In room ${room.displayName.get()}\n$message")
                        .owner(JFX.primaryStage)
                        .showWarning()
            }
        }
    }
}
