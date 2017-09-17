package koma.matrix.user.presence

import com.squareup.moshi.Json
import koma.matrix.UserId
import koma.model.user.UserState
import koma.storage.users.UserStore

enum class PresenceEventType {
    @Json(name = "m.presence") Presence
}

data class PresenceMessage(
        val sender:UserId,
        val type: PresenceEventType,
        val content: PresenceMessageContent) {

    fun getUserState(): UserState? {
        return UserStore.getOrCreateUserId(sender)
    }
}

data class PresenceMessageContent (
    val avatar_url: String? = null,
    val displayname: String? = null,
    val last_active_ago: Long?,
    val presence: UserPresenceType,
    val user_id: String?
    )

enum class UserPresenceType {
    @Json(name="online") Online,
    @Json(name="offline") Offline,
    @Json(name="unavailable") Unavailable,
}
