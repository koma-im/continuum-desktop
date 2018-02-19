package koma.controller.requests.membership

import javafx.scene.control.Alert
import koma.matrix.room.naming.RoomId
import koma_app.appState.apiClient
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import tornadofx.*
import koma.gui.view.window.roomfinder.RoomFinder
import kotlinx.coroutines.experimental.javafx.JavaFx as UI

fun ask_join_room() {
    val dialog = RoomFinder({ r -> joinRoomById(r)})
    dialog.open()
}

fun joinRoomById(roomid: String) {
    launch(UI) {
        val res = apiClient!!.joinRoom(RoomId(roomid)).awaitResult()
        when (res) {
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

