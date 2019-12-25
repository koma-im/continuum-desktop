package koma.gui.view.window.chatroom.roominfo.about.requests

import koma.koma_app.appState
import koma.matrix.event.room_message.state.RoomCanonAliasContent
import koma.matrix.room.naming.RoomAlias
import koma.util.onFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.desktop.Room
import link.continuum.desktop.gui.JFX
import org.controlsfx.control.Notifications

fun requestAddRoomAlias(room: Room, input: String?) {
    input?:return
    val api = appState.apiClient
    api ?: return
    val alias = RoomAlias(input)
    GlobalScope.launch {
        val result = api.putRoomAlias(room.id, alias.str)
        result.onFailure {
            val message = it.message
            launch(Dispatchers.JavaFx) {
                Notifications.create()
                        .title("Failed to add room alias $alias")
                        .text("In room ${room.displayName()}\n$message")
                        .owner(JFX.primaryStage)
                        .showWarning()
            }
        }
    }
}


fun requestSetRoomCanonicalAlias(room: Room, alias: String?) {
    requestSetRoomCanonicalAlias(room, alias?.let {RoomAlias(it)})
}
fun requestSetRoomCanonicalAlias(room: Room, alias: RoomAlias?) {
    alias?:return
    val api = appState.apiClient
    api ?: return
    val content =  RoomCanonAliasContent(alias)
    GlobalScope.launch {
        val result = api.setRoomCanonicalAlias(room.id, content)
        result.onFailure {
            val message = it.message
            launch(Dispatchers.JavaFx) {
                Notifications.create()
                        .title("Failed to set canonical room alias $alias")
                        .text("In room ${room.displayName()}\n$message")
                        .owner(JFX.primaryStage)
                        .showWarning()
            }
        }
    }
}
