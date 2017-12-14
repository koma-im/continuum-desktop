package controller

import javafx.event.ActionEvent
import koma.matrix.UserId
import matrix.UserRegistering
import model.Room
import rx.javafx.sources.CompositeObservable
import java.net.Proxy

/**
 * Created by developer on 2017/7/6.
 */
object guiEvents {
    val registerRequests = CompositeObservable<RegisterRequest>()
    val loginRequests = CompositeObservable<LoginRequest>()

    val createRoomRequests = CompositeObservable<ActionEvent>()
    val leaveRoomRequests = CompositeObservable<Room>()
    val uploadRoomIconRequests = CompositeObservable<Room>()
    val putRoomAliasRequests = CompositeObservable<Room>()
    val renameRoomRequests = CompositeObservable<Room>()

    val banMemberRequests = CompositeObservable<ActionEvent>()

    val sendImageRequests = CompositeObservable<Room>()

    val updateAvatar = CompositeObservable<ActionEvent>()
}

data class LoginRequest(
        val user: UserId,
        val server: String,
        val password: String?,
        val proxy: Proxy
)

data class RegisterRequest(
        val server: String,
        val proxy: Proxy,
        val usereg: UserRegistering
)
