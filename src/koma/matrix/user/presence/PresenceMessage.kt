package koma.matrix.user.presence

data class PresenceMessage(
    val type: String,
    val content: PresenceMessageContent)

data class PresenceMessageContent (
        // may be missing
    val avatar_url: String? = null,
    val displayname: String? = null,
    // should not be missing
    val last_active_ago: Long,
    val presence: String,
    val user_id: String
    )
