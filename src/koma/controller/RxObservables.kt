package controller

import matrix.UserRegistering
import javafx.event.ActionEvent
import koma.matrix.UserId
import model.Room
import rx.javafx.sources.CompositeObservable

/**
 * Created by developer on 2017/7/6.
 */
object guiEvents {
    val registerRequests = CompositeObservable<RegisterRequest>()
    val loginRequests = CompositeObservable<LoginRequest>()

    val createRoomRequests = CompositeObservable<ActionEvent>()
    val joinRoomRequests = CompositeObservable<ActionEvent>()
    val syncRoomRequests = CompositeObservable<String>()
    val leaveRoomRequests = CompositeObservable<Room>()
    val uploadRoomIconRequests = CompositeObservable<Room>()
    val putRoomAliasRequests = CompositeObservable<Room>()
    val renameRoomRequests = CompositeObservable<Room>()

    val inviteMemberRequests = CompositeObservable<ActionEvent>()
    val banMemberRequests = CompositeObservable<ActionEvent>()

    val sendMessageRequests = CompositeObservable<String>()
    val sendImageRequests = CompositeObservable<Room>()

    val updateAvatar = CompositeObservable<ActionEvent>()

    val statusMessage = CompositeObservable<String>()
}

data class LoginRequest(
        val user: UserId,
        val server: String,
        val password: String?
)

data class RegisterRequest(
        val server: String,
        val usereg: UserRegistering
)
