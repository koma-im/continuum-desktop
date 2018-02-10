package koma.storage.users

import koma.matrix.UserId
import koma.model.user.UserState
import koma.storage.users.state.load_user
import java.util.concurrent.ConcurrentHashMap

object UserStore {
    private val store = ConcurrentHashMap<UserId, UserState>()

    fun getOrCreateUserId(userId: UserId): UserState {
        val newUser = store.computeIfAbsent(userId, { load_user(it) })
        return newUser
    }
}

