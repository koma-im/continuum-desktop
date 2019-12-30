package koma.gui.view.window.chatroom.roominfo.about.requests

import javafx.stage.FileChooser
import koma.controller.requests.media.uploadFile
import koma.koma_app.appState
import koma.matrix.event.room_message.state.RoomAvatarContent
import koma.matrix.room.naming.RoomId
import koma.util.testFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.JFX
import org.controlsfx.control.Notifications

fun chooseUpdateRoomIcon(room: RoomId) {
    val api = appState.apiClient
    api ?: return
    val dialog = FileChooser()
    dialog.title = "Upload a icon for the room"
    val file = dialog.showOpenDialog(JFX.primaryStage)
    file ?: return
    GlobalScope.launch {
        val (success, failure, result) = uploadFile(api, file)
        if (!result.testFailure(success, failure)) {
            val icon = RoomAvatarContent(success.content_uri)
            val f = api.setRoomIcon(room, icon).failureOrNull()
            if (f!= null) {
                val message = f.message
                launch(Dispatchers.JavaFx) {
                    Notifications.create()
                            .title("Failed to set room icon")
                            .text("In room ${room}\n$message")
                            .owner(JFX.primaryStage)
                            .showWarning()
                }
            }
        }
    }
}
