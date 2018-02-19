package controller

import javafx.event.ActionEvent
import koma.matrix.UserId
import koma.storage.config.server.ServerConf
import matrix.UserRegistering
import model.Room
import rx.javafx.sources.CompositeObservable

/**
 * Created by developer on 2017/7/6.
 */
object guiEvents {
    val registerRequests = CompositeObservable<RegisterRequest>()
    val loginRequests = CompositeObservable<LoginRequest>()

    val createRoomRequests = CompositeObservable<ActionEvent>()
    val uploadRoomIconRequests = CompositeObservable<Room>()
    val putRoomAliasRequests = CompositeObservable<Room>()
    val renameRoomRequests = CompositeObservable<Room>()

    val updateAvatar = CompositeObservable<ActionEvent>()
}

data class LoginRequest(
        val user: UserId,
        val password: String?,
        val serverConf: ServerConf
)

data class RegisterRequest(
        val usereg: UserRegistering,
        val serverConf: ServerConf
)
