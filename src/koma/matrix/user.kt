package koma.matrix

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import koma.matrix.user.identity.UserId_new
import koma.storage.users.UserStore

data class UserId(
        val user: String,
        val server: String
) {
    override fun toString(): String {
        return "@$user:$server"
    }

    fun getState() = UserStore.getOrCreateUserId(this)
}

class UserIdAdapter {
    @ToJson fun toJson(userId: UserId): String {
        return userId.toString()
    }

    @FromJson fun fromJson(str: String) = UserId_new(str)
}

