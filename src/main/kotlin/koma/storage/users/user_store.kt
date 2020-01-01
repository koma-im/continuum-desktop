package koma.storage.users

import koma.matrix.UserId
import koma.model.user.UserState
import java.util.concurrent.ConcurrentHashMap

class UserStore {
    private val store = ConcurrentHashMap<UserId, UserState>()

    fun getOrCreateUserId(userId: UserId): UserState {
        val newUser = store.computeIfAbsent(userId) {
            val u = UserState(it)
            u
        }
        return newUser
    }
}

