package koma.gui.view.roomsview

import controller.guiEvents
import javafx.beans.property.Property
import javafx.scene.control.Alert
import javafx.scene.layout.HBox
import koma_app.appState
import kotlinx.coroutines.experimental.launch
import model.Room
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import rx.javafx.kt.actionEvents
import rx.javafx.kt.addTo
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
                            alert(Alert.AlertType.ERROR,
                                    "Error while leaving room ${roomname}",
                                    result.toString())
                        }
                    }
                }
            }
        }
        item("upload icon") {
            actionEvents().map { room.value }.addTo(guiEvents.uploadRoomIconRequests)
        }
        item("Add alias") {
            actionEvents().map { room.value }.addTo(guiEvents.putRoomAliasRequests)
        }
        item("Set alias") {
            actionEvents().map { room.value }.addTo(guiEvents.renameRoomRequests)
        }
    }
}

