package koma.controller.requests.membership

import com.github.kittinunf.result.Result
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import koma.matrix.room.naming.RoomId
import koma.matrix.user.identity.UserId_new
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma_app.appState.apiClient
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.*
import java.util.*

fun runAskBanRoomMember()  {
    val response = getBanningMember()
    if (!response.isPresent) {
        return
    }

    val room_user: Pair<String, String> = response.get()
    val roomid = room_user.first
    val username = room_user.second
    val userid = UserId_new(username)
    launch(JavaFx) {
        val result = apiClient!!.banMember(RoomId(roomid), userid).awaitMatrix()
        if (result is Result.Failure) {
            val content = result.error.message
            alert(Alert.AlertType.ERROR, "failed to ban $userid from $roomid", content)
        }
    }
}

private fun getBanningMember(): Optional<Pair<String, String>> {
    val dialog: Dialog<Pair<String, String>> = Dialog()
    dialog.setTitle("Ban Dialog")
    dialog.setHeaderText("Ban an enemy from a room")

    val inviteButtonType = ButtonType("Ban", ButtonBar.ButtonData.OK_DONE)
    dialog.getDialogPane().getButtonTypes().addAll(inviteButtonType, ButtonType.CANCEL)

    val grid = GridPane()

    val roomnamef = TextField()
    roomnamef.setPromptText("Room")
    val usernamef = TextField()
    usernamef.promptText = "User"

    grid.add(Label("Room:"), 0, 0)
    grid.add(roomnamef, 1, 0)
    grid.add(Label("user:"), 0, 1)
    grid.add(usernamef, 1, 1)

    val banButton = dialog.getDialogPane().lookupButton(inviteButtonType)
    banButton.setDisable(true)

    roomnamef.textProperty().addListener({ _, _, newValue ->
        banButton.setDisable(newValue.trim().isEmpty()) })

    dialog.getDialogPane().setContent(grid)

    dialog.setResultConverter({ dialogButton ->
        if (dialogButton === inviteButtonType) {
            return@setResultConverter Pair(roomnamef.getText(), usernamef.getText())
        }
        null
    })

    return dialog.showAndWait()
}
