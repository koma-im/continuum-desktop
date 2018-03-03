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


fun ask_invite_member() {
    val res = dialog_get_member()
    if (!res.isPresent) {
        return
    }

    val room_user: Pair<String, String> = res.get()
    val username = room_user.second
    val roomid = room_user.first

    val userid = UserId_new(username)
    launch(JavaFx) {
        val result = apiClient!!.inviteMember(RoomId(roomid), userid).awaitMatrix()
        if (result is Result.Failure) {
            val content = result.error.message
            alert(Alert.AlertType.ERROR, "failed to invite $userid to $roomid", content)
        }
    }
}

private fun dialog_get_member(): Optional<Pair<String, String>> {
    val dialog: Dialog<Pair<String, String>> = Dialog()
    dialog.setTitle("Invite Dialog")
    dialog.setHeaderText("Invite a friend to a room")

    val inviteButtonType = ButtonType("Ok", ButtonBar.ButtonData.OK_DONE)
    dialog.getDialogPane().getButtonTypes().addAll(inviteButtonType, ButtonType.CANCEL)

    val grid = GridPane()
    grid.hgap = 10.0
    grid.vgap = 10.0

    val roomnamef = TextField()
    roomnamef.setPromptText("Room")
    val usernamef = TextField()
    usernamef.promptText = "User"

    grid.add(Label("Room:"), 0, 0)
    grid.add(roomnamef, 1, 0)
    grid.add(Label("user:"), 0, 1)
    grid.add(usernamef, 1, 1)

    // Enable/Disable invite button depending on whether a username was entered.
    val inviteButton = dialog.getDialogPane().lookupButton(inviteButtonType)
    inviteButton.setDisable(true)

    // Do some validation (using the Java 8 lambda syntax).
    roomnamef.textProperty().addListener({ _, _, newValue ->
        inviteButton.setDisable(newValue.trim().isEmpty()) })

    dialog.getDialogPane().setContent(grid)

    // Convert the result to a username-password-pair when the login button is clicked.
    dialog.setResultConverter({ dialogButton ->
        if (dialogButton === inviteButtonType) {
            return@setResultConverter Pair(roomnamef.getText(), usernamef.getText())
        }
        null
    })

    return dialog.showAndWait()
}
