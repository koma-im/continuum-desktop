package koma.controller.requests.membership

import javafx.scene.control.Alert
import koma.matrix.room.naming.RoomId
import koma_app.appState.apiClient
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import tornadofx.*
import view.dialog.FindJoinRoomDialog
import kotlinx.coroutines.experimental.javafx.JavaFx as UI

fun ask_join_room() {
    val dialog = FindJoinRoomDialog()

    val result = dialog.showAndWait()

    if (!result.isPresent()) {
        println("no room selected to join")
        return
    } else {
        println("now join room ${result.get()}")
    }
    val roomid = result.get()

    launch(UI) {
        val res = apiClient!!.joinRoom(RoomId(roomid)).awaitResult()
        when(res) {
            is Result.Ok -> {}
            is Result.Error -> {
                val content = "http error ${res.exception.code()}: ${res.exception.message()}"
                alert(Alert.AlertType.ERROR, "Room joining failed; room might be private", content)
            }
            is Result.Exception -> {
                val content = res.exception.localizedMessage
                alert(Alert.AlertType.ERROR, "failed to join $roomid", content)
                res.exception.printStackTrace()
            }
        }
    }
}

