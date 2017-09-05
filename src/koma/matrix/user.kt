package koma.matrix

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson
import koma.matrix.user.identity.UserId_new

data class UserId(
        val user: String,
        val server: String
) {
    override fun toString(): String {
        return "@$user:$server"
    }
}

class UserIdAdapter {
    @ToJson fun toJson(userId: UserId): String {
        return userId.toString()
    }

    @FromJson fun fromJson(str: String): UserId {
        val uid = UserId_new(str)
        if (uid != null)
            return uid
        else
            throw JsonDataException("Not a valid user: $str")
    }
}

