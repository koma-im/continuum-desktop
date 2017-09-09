package koma.storage.users

import javafx.scene.control.Alert
import koma.matrix.UserId
import koma.matrix.user.identity.UserId_new
import koma.model.user.UserState
import tornadofx.*

object UserStore {
    private val store = HashMap<UserId, UserState>()

    @Synchronized
    fun getOrCreateUserId(userId: UserId): UserState {
        val existingUser = store.get(userId)
        if (existingUser != null)
            return existingUser
        val newUser = UserState(userId)
        store.put(userId, newUser)
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

}

