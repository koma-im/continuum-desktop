package koma.storage.users

import koma.matrix.UserId
import koma.model.user.UserState
import koma.storage.config.ConfigPaths
import koma.storage.users.state.load_user
import java.util.concurrent.ConcurrentHashMap

class UserStore(private val paths: ConfigPaths) {
    private val store = ConcurrentHashMap<UserId, UserState>()

    fun getOrCreateUserId(userId: UserId): UserState {
        val newUser = store.computeIfAbsent(userId, { load_user(it, paths) })
        return newUser
    }
}

