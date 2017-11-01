package koma.matrix.room.participation

import com.squareup.moshi.Json

enum class RoomJoinRules {
    @Json(name = "public")
    Public,
    Knock,
    @Json(name = "invite")
    Invite,
    Private;

    companion object {
        fun fromString(rule: String): RoomJoinRules? {
            val join = when (rule) {
                "public" -> RoomJoinRules.Public
                "invite" -> RoomJoinRules.Invite
            // not used on the matrix network for now
                "knock" -> RoomJoinRules.Knock
                "private" -> RoomJoinRules.Private
                else -> null
            }
            return join
        }
    }
}
