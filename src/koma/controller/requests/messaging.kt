package koma.controller.requests

import domain.UploadResponse
import javafx.scene.control.Alert
import javafx.stage.FileChooser
import koma.matrix.room.naming.RoomId
import koma_app.appState.apiClient
import kotlinx.coroutines.experimental.launch
import okhttp3.MediaType
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import tornadofx.*
import kotlinx.coroutines.experimental.javafx.JavaFx as UI

fun sendMessage(room: RoomId, message: String) {
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

fun sendFileMessage(room: RoomId) {
    val dialog = FileChooser()
    dialog.title = "Find a file to send"
    val file = dialog.showOpenDialog(FX.primaryStage)

    if (file == null) {
        return
    }
    val type = MediaType.parse("application/octet-stream")!!
    val api = apiClient
    api?:return
    launch {
        val uploadResult = api.uploadFile(file, type).awaitResult()
        if (uploadResult is Result.Error) {
            val error = "during upload: http error ${uploadResult.exception.code()}: ${uploadResult.exception.message()}"
            launch(kotlinx.coroutines.experimental.javafx.JavaFx) {
                alert(Alert.AlertType.ERROR, error)
            }
            return@launch
        }
        if (uploadResult is Result.Exception) {
            val ex = uploadResult.exception
            launch(kotlinx.coroutines.experimental.javafx.JavaFx) {
                alert(Alert.AlertType.ERROR, "during upload exception $ex, ${ex.cause}")
            }
            return@launch
        }
        if (uploadResult is Result.Ok) {
            val up: UploadResponse = uploadResult.value
            println("sending $file ${up.content_uri}")
            api.sendFile(room, file.name, up.content_uri).awaitResult()
        }
    }
}
