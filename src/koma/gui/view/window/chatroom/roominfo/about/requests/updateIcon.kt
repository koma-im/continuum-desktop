package koma.gui.view.window.chatroom.roominfo.about.requests

import com.github.kittinunf.result.Result
import javafx.stage.FileChooser
import koma.controller.requests.media.uploadFile
import koma.matrix.event.room_message.state.RoomAvatarContent
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma.koma_app.appState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import model.Room
import org.controlsfx.control.Notifications
import tornadofx.*

fun chooseUpdateRoomIcon(room: Room) {
    val api = appState.apiClient
    api ?: return
    val dialog = FileChooser()
    dialog.title = "Upload a icon for the room"
    val file = dialog.showOpenDialog(FX.primaryStage)
    file ?: return
    GlobalScope.launch {
        val upload = uploadFile(api, file)
        if (upload is Result.Success) {
            val icon = RoomAvatarContent(upload.value.content_uri)
            val result = api.setRoomIcon(room.id, icon).awaitMatrix()
            if (result is Result.Failure) {
                val message = result.error.message
                launch(Dispatchers.JavaFx) {
                    Notifications.create()
                            .title("Failed to set room icon")
                            .text("In room ${room.displayName.get()}\n$message")
                            .owner(FX.primaryStage)
                            .showWarning()
                }
            }
        }
    }
}
