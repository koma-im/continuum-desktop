package koma.gui.view.roomsview

import javafx.beans.property.Property
import javafx.scene.control.Alert
import javafx.scene.layout.HBox
import koma.gui.dialog.room.RoomInfoDialog
import koma_app.appState
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import model.Room
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import tornadofx.*


fun addMenu(node: HBox, room: Property<Room>) {
    node.contextmenu {
        item("leave") {
            action {
                launch {
                    val api = appState.apiClient
                    api ?: return@launch
                    val mxroom = room.value
                    val roomname = mxroom.displayName.get()
                    println("Leaving $roomname")
                    val result = api.leavingRoom(mxroom.id).awaitResult()
                    when(result) {
                        is Result.Error, is Result.Exception -> {
                            launch(JavaFx) {
                                alert(Alert.AlertType.ERROR,
                                        "Error while leaving room ${roomname}",
                                        result.toString())
                            }
                        }
                    }
                }
            }
        }
        item("Room Info") { action {
            RoomInfoDialog(room.value).openWindow() } }
    }
}

