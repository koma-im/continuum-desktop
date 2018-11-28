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
    @Json(name = "pinned-events") PinnedEvents,
    @Json(name = "m.room.bot.options") BotOptions,

    @Json(name = "m.room.history_visibility") HistoryVisibility,

    @Json(name = "m.room.guest_access") GuestAccess;

    override fun toString(): String {
        return enumToStr(this)
    }

    companion object {
        private val enumStrMap = values().map { it to findJsonAnnotationStr(it) }.toMap()
        private val strEnumMap = enumStrMap.entries
                .map { Pair(it.value, it.key) }.toMap()
        private fun findJsonAnnotationStr(t: RoomEventType): String {
            val am = RoomEventType::class.java.getField(t.name).getAnnotation(Json::class.java)
            return am.name
        }

        fun enumToStr(t: RoomEventType): String {
            return enumStrMap[t] !!
        }
        fun strToEnum(s: String): RoomEventType? {
            return strEnumMap[s]
        }
    }
}
