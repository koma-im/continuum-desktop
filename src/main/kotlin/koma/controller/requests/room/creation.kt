package koma.controller.requests.room

import javafx.geometry.Pos
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.TextField
import koma.koma_app.appState.apiClient
import koma.matrix.room.admin.CreateRoomSettings
import koma.matrix.room.visibility.RoomVisibility
import koma.util.onFailure
import koma.util.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.JFX
import org.controlsfx.control.Notifications
import tornadofx.*

fun createRoomInteractive() = GlobalScope.launch(Dispatchers.JavaFx) {
    val input = RoomCreationDialog().showAndWait()
    if (!input.isPresent) return@launch
    val settings = input.get()
    val api = apiClient ?: return@launch
    val result = api.createRoom(settings)
    result.onFailure {
        launch(Dispatchers.JavaFx) {
            Notifications.create()
                    .owner(JFX.primaryStage)
                    .position(Pos.CENTER)
                    .title("Failure to create room ${settings.room_alias_name}")
                    .text(it.toString())
                    .showWarning()
        }
    }.onSuccess{
        println("Room created ${settings.room_alias_name}")
    }
}

private class RoomCreationDialog(): Dialog<CreateRoomSettings?>() {

    private val roomnamef = TextField()
    private val visibilityChoice = combobox(values = RoomVisibility.values().toList())

    init {
        this.setTitle("Creation Dialog")
        this.setHeaderText("Create a room")

        val createButtonType = ButtonType("Create", ButtonBar.ButtonData.OK_DONE)
        this.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL)

        roomnamef.setPromptText("Room")

        this.dialogPane.content = form {
            fieldset {
                field("Room:") {
                    add(roomnamef)
                }
                field("Visibility:") {
                    add(visibilityChoice)
                }
            }
        }

        val creationButton = this.getDialogPane().lookupButton(createButtonType)
        creationButton.disableWhen {
            roomnamef.textProperty().isEmpty.or(visibilityChoice.valueProperty().isNull)
        }

        this.setResultConverter({ dialogButton ->
            if (dialogButton === createButtonType) {
                return@setResultConverter computeResult()
            }
            null
        })
    }

    private fun computeResult(): CreateRoomSettings {
        val name = roomnamef.text
        val visibility = this.visibilityChoice.value
        return CreateRoomSettings(name, visibility)
    }
}

