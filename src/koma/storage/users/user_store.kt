package koma.storage.users

import koma.matrix.UserId
import koma.model.user.UserState
import link.continuum.desktop.database.KDataStore
import link.continuum.desktop.database.models.getLatestAvatar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class UserStore(private val data: KDataStore) {
    private val store = ConcurrentHashMap<UserId, UserState>()

    fun getOrCreateUserId(userId: UserId): UserState {
        val newUser = store.computeIfAbsent(userId) {
            val u = UserState(it, data)
            val av = getLatestAvatar(data, it)
            if (av != null) {
                val url = HttpUrl.parse(av.avatar)
                if (url!=null) u.setAvatar(url, av.since)
            }
            u
        }
        return newUser
    }
}

