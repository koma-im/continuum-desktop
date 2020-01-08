package koma.controller.requests.membership

import koma.HttpFailure
import koma.gui.view.window.auth.uilaunch
import koma.koma_app.AppData
import koma.koma_app.appState
import koma.matrix.room.naming.RoomId
import koma.util.failureOrThrow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.JFX
import link.continuum.desktop.util.Account
import link.continuum.desktop.util.debugAssertUiThread
import mu.KotlinLogging
import org.controlsfx.control.Notifications

private val logger = KotlinLogging.logger {}

suspend fun forgetRoom(api: Account, roomId: RoomId, appData: AppData) {
    logger.debug { "forgetting $roomId" }
    debugAssertUiThread()
    val roomname = roomId
    val result = api.forgetRoom(roomId)
    when {
        result.isSuccess -> {
            logger.debug { "forgot $roomname successfully" }
        }
        result.isFailure -> {
            val ex = result.failureOrThrow()
            Notifications.create()
                    .title("Had error forgetting room $roomname")
                    .text("$ex")
                    .owner(JFX.primaryStage)
                    .showWarning()
        }
    }

}

@ExperimentalCoroutinesApi
fun leaveRoom(mxroom: RoomId, appData: AppData) {
    val roomId = mxroom
    logger.debug { "Leaving $roomId" }
    val api = appState.apiClient
    api ?: return
    val myRooms = appData.keyValueStore.roomsOf(api.userId)
    GlobalScope.launch {
        val roomname = mxroom.localstr
        val result = api.leavingRoom(roomId)
        when {
            result.isSuccess -> {
                logger.debug { "Left $roomname successfully" }
                myRooms.leave(listOf(roomId))
            }
            result.isFailure -> {
                val ex = result.failureOrThrow()
                uilaunch {
                    Notifications.create()
                            .title("Had error leaving room $roomname")
                            .text("$ex")
                            .owner(JFX.primaryStage)
                            .showWarning()
                    if ((ex is HttpFailure && ex.http_code == 404)) {
                        logger.debug { "leaving room although there is exception $ex" }
                        myRooms.leave(listOf(roomId))
                    }
                }
            }
        }
    }
}
