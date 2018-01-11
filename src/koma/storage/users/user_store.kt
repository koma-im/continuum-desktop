package koma.storage.users

import javafx.scene.control.Alert
import koma.matrix.UserId
import koma.matrix.user.identity.UserId_new
import koma.model.user.UserState
import koma.storage.users.state.load_user
import koma.storage.users.state.save
import tornadofx.*
import java.util.concurrent.ConcurrentHashMap

object UserStore {
    private val store = ConcurrentHashMap<UserId, UserState>()

    fun getOrCreateUserId(userId: UserId): UserState {
        val newUser = store.computeIfAbsent(userId, { load_user(it) })
        return newUser
    }

    fun getOrCreateUser(useridstr: String): UserState? {
        val userid = UserId_new(useridstr)
        if (userid == null) {
            alert(Alert.AlertType.WARNING, "Invalid user id")
            return null
        } else
            return getOrCreateUserId(userid)
    }

    init {

        Runtime.getRuntime().addShutdownHook(Thread({
            this.store.forEach { _, u: UserState -> u.save() }
        }))
    }
}

