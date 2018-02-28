package koma.controller.requests.membership

import javafx.scene.control.Alert
import koma_app.appState
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import model.Room
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import tornadofx.*

fun leaveRoom(mxroom: Room) {
    val api = appState.apiClient
    api ?: return
    launch {
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
