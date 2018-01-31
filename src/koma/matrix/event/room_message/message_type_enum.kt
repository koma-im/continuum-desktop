package matrix.event.room_message

import com.squareup.moshi.Json

enum class RoomEventType{
    @Json(name = "m.room.aliases") Aliases,
    @Json(name = "m.room.canonical_alias") CanonAlias,
    @Json(name = "m.room.create") Create,
    @Json(name = "m.room.join_rules") JoinRule,
    @Json(name = "m.room.power_levels") PowerLevels,
    @Json(name = "m.room.member") Member,
    @Json(name = "m.room.message") Message,
    @Json(name = "m.room.redaction") Redaction,

    @Json(name = "m.room.name") Name,
    @Json(name = "m.room.topic") Topic,
    @Json(name = "m.room.avatar") Avatar,

    @Json(name = "m.room.history_visibility") HistoryVisibility,

    @Json(name = "m.room.guest_access") GuestAccess;

    override fun toString(): String {
        return when(this) {
            Aliases -> "m.room.aliases"
            CanonAlias ->         "m.room.canonical_alias"
            Create ->             "m.room.create"
            JoinRule ->           "m.room.join_rules"
            PowerLevels ->        "m.room.power_levels"
            Member ->             "m.room.member"
            Message ->            "m.room.message"
            Redaction ->          "m.room.redaction"
            Name ->               "m.room.name"
            Topic ->              "m.room.topic"
            Avatar ->             "m.room.avatar"
            HistoryVisibility ->  "m.room.history_visibility"
            GuestAccess ->        "m.room.guest_access"
        }
    }
}
