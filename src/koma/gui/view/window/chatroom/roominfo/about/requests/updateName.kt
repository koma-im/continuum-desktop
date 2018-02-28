package koma.gui.view.window.chatroom.roominfo.about.requests

import com.github.kittinunf.result.Result
import koma.matrix.event.room_message.state.RoomNameContent
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma_app.appState
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import model.Room
import org.controlsfx.control.Notifications
import tornadofx.*

fun requestUpdateRoomName(room: Room, input: String?) {
    input?:return
    val api = appState.apiClient
    api ?: return
    val name = RoomNameContent(input)
    launch {
        val result = api.setRoomName(room.id, name).awaitMatrix()
        if (result is Result.Failure) {
            val message = result.error.message
            launch(JavaFx) {
                Notifications.create()
                        .title("Failed to set room name $name")
                        .text("In room ${room.displayName.get()}\n$message")
                        .owner(FX.primaryStage)
                        .showWarning()
            }
        }
    }
}
