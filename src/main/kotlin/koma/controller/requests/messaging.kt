package koma.controller.requests

import javafx.scene.control.Alert
import javafx.stage.FileChooser
import koma.controller.requests.media.uploadFile
import koma.koma_app.appState.apiClient
import koma.matrix.event.room_message.chat.FileInfo
import koma.matrix.event.room_message.chat.FileMessage
import koma.matrix.event.room_message.chat.ImageMessage
import koma.matrix.event.room_message.chat.textToMessage
import koma.matrix.room.naming.RoomId
import koma.util.file.guessMediaType
import koma.util.onFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import link.continuum.desktop.gui.JFX
import link.continuum.desktop.util.gui.alert
import okhttp3.MediaType
import org.controlsfx.control.Notifications
import kotlinx.coroutines.javafx.JavaFx as UI

fun sendMessage(room: RoomId, message: String) {
    val msg = textToMessage(message)
    GlobalScope.launch(Dispatchers.UI) {
        val result = apiClient!!.sendMessage(room, msg)
        result.onFailure {
            val content = it.toString()
            alert(Alert.AlertType.ERROR, "failed to send message", content)
        }
    }
}

fun sendFileMessage(room: RoomId) {
    val dialog = FileChooser()
    dialog.title = "Find a file to send"

    val file = dialog.showOpenDialog(JFX.primaryStage)
    file?:return

    val type = file.guessMediaType() ?: MediaType.parse("application/octet-stream")!!
    val api = apiClient
    api?:return
    GlobalScope.launch {
        val up = uploadFile(api, file, type).getOrNull() ?: return@launch
            println("sending $file ${up.content_uri}")
            val fileinfo = FileInfo(type.toString(), file.length())
            val message = FileMessage(file.name, up.content_uri, fileinfo)
            val r = api.sendMessage(room, message).failureOrNull()
            if (r != null) {
                withContext(Dispatchers.JavaFx) {
                    Notifications.create()
                            .title("Failed to send file $file")
                            .text("Error ${r}")
                            .showError()
                }
            }
    }
}

fun sendImageMessage(room: RoomId) {
    val dialog = FileChooser()
    dialog.title = "Find an image to send"

    val file = dialog.showOpenDialog(JFX.primaryStage)
    file?:return

    val type = file.guessMediaType() ?: MediaType.parse("application/octet-stream")!!
    val api = apiClient
    api?:return
    GlobalScope.launch {
        val up = uploadFile(api, file, type).getOrNull()
        if (up != null) {
            println("sending image $file ${up.content_uri}")
            val msg = ImageMessage(file.name, up.content_uri)
            val it = api.sendMessage(room, msg).failureOrNull() ?: return@launch
            withContext(Dispatchers.Main) {
                    Notifications.create()
                            .title("Failed to send image $file")
                            .text("Error ${it}")
                            .showError()
            }
        }
    }
}
