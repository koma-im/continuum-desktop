package koma.gui.view.window.chatroom.roominfo.about.requests

import koma.koma_app.appState
import koma.matrix.event.room_message.state.RoomNameContent
import koma.matrix.room.naming.RoomId
import koma.util.onFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.JFX
import org.controlsfx.control.Notifications

fun requestUpdateRoomName(room: RoomId, input: String?) {
    input?:return
    val api = appState.apiClient
    api ?: return
    val name = RoomNameContent(input)
    GlobalScope.launch {
        val result = api.setRoomName(room, name)
        result.onFailure {
            val message = it.message
            launch(Dispatchers.JavaFx) {
                Notifications.create()
                        .title("Failed to set room name $name")
                        .text("In room ${room}\n$message")
                        .owner(JFX.primaryStage)
                        .showWarning()
            }
        }
    }
}
