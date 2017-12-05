package koma.controller.requests

import javafx.scene.control.Alert
import koma_app.appState.apiClient
import kotlinx.coroutines.experimental.launch
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import tornadofx.*
import kotlinx.coroutines.experimental.javafx.JavaFx as UI

fun sendMessage(room: String, message: String) {
    val resultsend = apiClient!!.sendMessage(room, message)
    launch(UI) {
        val result = resultsend.awaitResult()
        when (result) {
            is Result.Ok -> println("message $message sent: ${result.value}")
            is Result.Error -> {
                val content = "http error ${result.exception.code()}: ${result.exception.message()}"
                alert(Alert.AlertType.ERROR, "failed to send message", content)
            }
            is Result.Exception -> {
                val content = result.exception.localizedMessage
                alert(Alert.AlertType.ERROR, "failed to send message", content)
            }
        }
    }
}
