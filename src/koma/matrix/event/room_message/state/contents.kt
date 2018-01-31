package koma.matrix.event.room_message.state

import com.squareup.moshi.Json
import koma.matrix.event.room_message.chat.ImageInfo
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility


class RoomAliasesContent(
        val aliases: List<RoomAlias>
)

class RoomCanonAliasContent(
        val alias: RoomAlias
)

class RoomHistoryVisibilityContent(
        val history_visibility: HistoryVisibility
)

class RoomPowerLevelsContent(
        val users_default: Float = 0.0f,
        /**
         *  state_default Defaults to 50 if unspecified,
         * but 0 if there is no m.room.power_levels event at all.
         */
        val state_default: Float = 50.0f,
        val events_default: Float = 0.0f,
        val ban: Float = 50f,
        val invite: Float = 50f,
        val kick: Float = 50f,
        val redact: Float = 50f,
        val events: Map<String, Float>,
        val users: Map<String, Float>
)


class RoomJoinRulesContent(
        val join_rule: RoomJoinRules
)

class RoomRedactContent(
        val reason: String
)

class RoomCreateContent(
        val creator: String,
        @Json(name = "m.federate") val federate: Boolean = true
)

class RoomNameContent(
        val name: String?
)

class RoomTopicContent(
        val topic: String
)

class RoomAvatarContent(
        val url: String,
        val info: ImageInfo?
)

class RoomPinnedEventsContent(
        val pinned: List<String>
)
