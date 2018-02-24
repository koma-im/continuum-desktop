package controller

import javafx.event.ActionEvent
import koma.storage.config.server.ServerConf
import matrix.UserRegistering
import model.Room
import rx.javafx.sources.CompositeObservable

/**
 * Created by developer on 2017/7/6.
 */
object guiEvents {
    val registerRequests = CompositeObservable<RegisterRequest>()

    val uploadRoomIconRequests = CompositeObservable<Room>()
    val putRoomAliasRequests = CompositeObservable<Room>()
    val renameRoomRequests = CompositeObservable<Room>()

    val updateAvatar = CompositeObservable<ActionEvent>()
}

data class RegisterRequest(
        val usereg: UserRegistering,
        val serverConf: ServerConf
)
