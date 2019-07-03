package koma.controller.requests.membership

import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.gui.view.window.auth.uilaunch
import koma.matrix.room.naming.RoomId
import koma.matrix.user.identity.UserId_new
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import koma.koma_app.appState.apiClient
import koma.util.getOr
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tornadofx.*
import java.util.*


fun dialogInviteMember(roomId: RoomId) {
    val res = dialog_get_member()
    if (!res.isPresent) {
        return
    }
    val username = res.get()

    val userid = UserId_new(username)
    GlobalScope.launch {
        apiClient!!.inviteMember(roomId, userid).awaitMatrix() getOr {
            uilaunch {
                val content = it.toString()
                alert(Alert.AlertType.ERROR, "failed to invite $userid to $roomId", content)
            }
            return@launch
        }
    }
}

private fun dialog_get_member(): Optional<String> {
    val dialog = Dialog<String>()
    dialog.title = "Send an invitation"
    dialog.headerText = "Invite someone to the room"

    val inviteButtonType = ButtonType("Ok", ButtonBar.ButtonData.OK_DONE)
    dialog.dialogPane.buttonTypes.addAll(inviteButtonType, ButtonType.CANCEL)

    val usernamef = TextField()
    usernamef.promptText = "e.g. @chris:matrix.org"
    dialog.dialogPane.content = VBox(5.0).apply {
        vgrow = Priority.ALWAYS
        hbox(5.0) {
            alignment = Pos.CENTER
            label("User ID:")
            add(usernamef)
        }
    }

    val inviteButton = dialog.dialogPane.lookupButton(inviteButtonType)
    inviteButton.isDisable = true

    usernamef.textProperty().addListener { _, _, newValue ->
        inviteButton.isDisable = newValue.trim().isEmpty()
    }

    // Convert the result to a username-password-pair when the login button is clicked.
    dialog.setResultConverter { dialogButton ->
        if (dialogButton === inviteButtonType) {
            return@setResultConverter usernamef.getText()
        }
        null
    }

    return dialog.showAndWait()
}
