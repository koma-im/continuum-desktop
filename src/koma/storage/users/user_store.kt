package koma.storage.users

import koma.matrix.UserId
import koma.model.user.UserState
import link.continuum.database.KDataStore
import java.util.concurrent.ConcurrentHashMap

class UserStore(private val data: KDataStore) {
    private val store = ConcurrentHashMap<UserId, UserState>()

    fun getOrCreateUserId(userId: UserId): UserState {
        val newUser = store.computeIfAbsent(userId) {
            val u = UserState(it, data)
            u
        }
        return newUser
    }
}

