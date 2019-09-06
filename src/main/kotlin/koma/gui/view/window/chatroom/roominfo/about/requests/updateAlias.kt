package koma.gui.view.window.chatroom.roominfo.about.requests

import koma.matrix.event.room_message.state.RoomCanonAliasContent
import koma.matrix.room.naming.RoomAlias
import koma.koma_app.appState
import koma.util.onFailure
import koma.util.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.JFX
import model.Room
import org.controlsfx.control.Notifications
import tornadofx.*

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
                        .text("In room ${room.displayName.get()}\n$message")
                        .owner(JFX.primaryStage)
                        .showWarning()
            }
        }.onSuccess {
            launch(Dispatchers.JavaFx) {
                room.addAlias(alias)
            }
        }
    }
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
                        .text("In room ${room.displayName.get()}\n$message")
                        .owner(JFX.primaryStage)
                        .showWarning()
            }
        }
    }
}
