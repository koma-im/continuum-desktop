package koma.matrix.user.presence

import koma.matrix.UserId
import koma.model.user.UserState
import koma.storage.users.UserStore

data class PresenceMessage(
        val sender:UserId,
    val type: String,
        val content: PresenceMessageContent) {

    fun getUserState(): UserState? {
        return UserStore.getOrCreateUserId(sender)
    }
}

data class PresenceMessageContent (
    val avatar_url: String? = null,
    val displayname: String? = null,
    val last_active_ago: Long?,
    val presence: String,
    val user_id: String?
    )
