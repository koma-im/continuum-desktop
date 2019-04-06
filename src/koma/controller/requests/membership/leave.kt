package koma.controller.requests.membership

import com.github.kittinunf.result.Result
import koma.koma_app.appState
import koma.util.coroutine.adapter.retrofit.HttpException
import koma.util.coroutine.adapter.retrofit.awaitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import model.Room
import org.controlsfx.control.Notifications
import tornadofx.*

fun leaveRoom(mxroom: Room) {
    val api = appState.apiClient
    api ?: return
    val removeLocally = { GlobalScope.launch(Dispatchers.JavaFx) {
        val u = appState.currentUser ?: return@launch
        appState.store.getAccountRoomStore(u).remove(mxroom.id)
    } }
    GlobalScope.launch {
        val roomname = mxroom.displayName.get()
        println("Leaving $roomname")
        val result = api.leavingRoom(mxroom.id).awaitMatrix()
        when(result) {
            is Result.Success -> { removeLocally() }
            is Result.Failure -> {
                val ex = result.error
                launch(Dispatchers.JavaFx) {
                    Notifications.create()
                            .title("Had error leaving room $roomname")
                            .text("${ex.message}")
                            .owner(FX.primaryStage)
                            .showWarning()
                }
                if ((ex is HttpException && ex.code == 404)) {
                    System.err.println()
                    removeLocally()
                }
            }
        }
    }
}
