package koma.gui.view.window.userinfo.actions

import com.github.kittinunf.result.Result
import javafx.stage.FileChooser
import koma.controller.requests.media.uploadFile
import koma.matrix.event.room_message.state.RoomAvatarContent
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma_app.appState
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import model.Room
import org.controlsfx.control.Notifications
import tornadofx.*

fun chooseUpdateUserAvatar(room: Room) {
    val api = appState.apiClient
    api ?: return

            .subscribe {
                apiClient.updateAvatar(apiClient.userId, AvatarUrl(it))
            }


    val dialog = FileChooser()
    dialog.title = "Upload a new avatar"
    val file = dialog.showOpenDialog(FX.primaryStage)
    file ?: return
    launch {
        val upload = uploadFile(api, file)
        if (upload is Result.Success) {
            api.updateAvatar()
            val icon = RoomAvatarContent(upload.value.content_uri)
            val result = api.setRoomIcon(room.id, icon).awaitMatrix()
            if (result is Result.Failure) {
                val message = result.error.message
                launch(JavaFx) {
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
