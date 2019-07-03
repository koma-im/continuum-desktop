package koma.gui.view.window.chatroom.roominfo.about.requests

import javafx.stage.FileChooser
import koma.controller.requests.media.uploadFile
import koma.matrix.event.room_message.state.RoomAvatarContent
import koma.koma_app.appState
import koma.util.onFailure
import koma.util.onSuccess
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
        upload.onSuccess {
            val icon = RoomAvatarContent(it.content_uri)
            val result = api.setRoomIcon(room.id, icon)
            result.onFailure {
                val message = it.message
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
