package koma.gui.view.window.chatroom.roominfo.about.requests

import koma.koma_app.appState
import koma.matrix.event.room_message.state.RoomCanonAliasContent
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.naming.RoomId
import koma.util.onFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.JFX
import org.controlsfx.control.Notifications

fun requestAddRoomAlias(room: RoomId, input: String?) {
    input?:return
    val api = appState.apiClient
    api ?: return
    val alias = RoomAlias(input)
    GlobalScope.launch {
        val result = api.putRoomAlias(room, alias.str)
        result.onFailure {
            val message = it.message
            launch(Dispatchers.JavaFx) {
                Notifications.create()
                        .title("Failed to add room alias $alias")
                        .text("In room ${room}\n$message")
                        .owner(JFX.primaryStage)
                        .showWarning()
            }
        }
    }
}


fun requestSetRoomCanonicalAlias(
        room: RoomId, alias: RoomAlias) {
    val api = appState.apiClient
    api ?: return
    val content =  RoomCanonAliasContent(alias)
    GlobalScope.launch {
        val result = api.setRoomCanonicalAlias(room, content)
        result.onFailure {
            val message = it.message
            launch(Dispatchers.JavaFx) {
                val notification = Notifications.create()
                        .title("Failed to set canonical room alias $alias")
                        .text("In room ${room}\n$message")
                        .owner(JFX.primaryStage)
                notification.showWarning()
            }
        }
    }
}
