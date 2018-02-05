package koma.controller.requests

import domain.UploadResponse
import javafx.scene.control.Alert
import javafx.stage.FileChooser
import koma.controller.requests.media.uploadFile
import koma.matrix.event.room_message.chat.FileInfo
import koma.matrix.event.room_message.chat.FileMessage
import koma.matrix.event.room_message.chat.ImageMessage
import koma.matrix.room.naming.RoomId
import koma.util.file.guessMediaType
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
    file?:return

    val type = file.guessMediaType() ?: MediaType.parse("application/octet-stream")!!
    val api = apiClient
    api?:return
    launch {
        val uploadResult = uploadFile(api, file, type)
        if (uploadResult is Result.Ok) {
            val up: UploadResponse = uploadResult.value
            println("sending $file ${up.content_uri}")
            val fileinfo = FileInfo(type.toString(), file.length())
            val message = FileMessage(file.name, up.content_uri, fileinfo)
            api.sendRoomMessage(room, message).awaitResult()
        }
    }
}

fun sendImageMessage(room: RoomId) {
    val dialog = FileChooser()
    dialog.title = "Find an image to send"

    val file = dialog.showOpenDialog(FX.primaryStage)
    file?:return

    val type = file.guessMediaType() ?: MediaType.parse("application/octet-stream")!!
    val api = apiClient
    api?:return
    launch {
        val uploadResult = uploadFile(api, file, type)
        if (uploadResult is Result.Ok) {
            val up: UploadResponse = uploadResult.value
            println("sending image $file ${up.content_uri}")
            val msg = ImageMessage(file.name, up.content_uri)
            api.sendRoomMessage(room, msg).awaitResult()
        }
    }
}
