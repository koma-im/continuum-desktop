package koma.matrix.event.room_message

import koma.matrix.event.room_message.chat.ChatContent
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.sync.RawMessage
import koma.model.user.UserState
import koma.storage.users.UserStore
import java.util.*

/**
 * i don't want to put all subclasses in this file, but kotlin requires it
 */
sealed class RoomMessage() {
    abstract val time: Long
}

data class MemberJoinMsg(
        val sender: UserState,
        val datetime: Date,
        override val time: Long,
        val name: String?,
        val avatar_url: String?
): RoomMessage()

data class MemberUpdateMsg(
        val sender: UserState,
        val datetime: Date,
        override val time: Long,
        val name_change: Pair<String?, String?>,
        val avatar_change: Pair<String?, String?>
): RoomMessage()

data class MemberLeave(
        val sender: UserState,
        override val time: Long,
        val datetime: Date
): RoomMessage()

data class MemberBan(
        val sender: UserState,
        override val time: Long,
        val datetime: Date
): RoomMessage()


data class MemberJoin(
        val sender: UserState,
        override val time: Long,
        val datetime: Date
): RoomMessage()

data class RoomCreationMsg(
        val sender: UserState,
        override val time: Long,
        val datetime: Date
        ): RoomMessage() {
    companion object {
        fun fromMessage(message: RawMessage): RoomCreationMsg {
            val sender = UserStore.getOrCreateUserId(message.sender)
            val datetime: Date = Date(message.origin_server_ts)
            val msg = RoomCreationMsg(sender, message.origin_server_ts, datetime)
            return msg
        }
    }
}

/**
 * currently only used to update the state of a Room
 */
data class RoomAliasUpdate(
        override val time: Long,
        val aliases: List<RoomAlias>
): RoomMessage()

data class RoomIconUpdate(
        override val time: Long,
        val url: String
): RoomMessage()


data class RoomNameUpdate(
        override val time: Long,
        val name: String
): RoomMessage()


data class RoomGuestAccess(
        override val time: Long,
        val content: Map<String, Any>
): RoomMessage()


data class RoomRedaction(
        override val time: Long,
        val content: Map<String, Any>
): RoomMessage()

data class RoomTopic(
        override val time: Long,
        val content: Map<String, Any>
): RoomMessage()

data class RoomPowerLevel(
        override val time: Long,
        val powerLevels: Map<String, Int>,
        val userLevels: Map<String, Double>
): RoomMessage()

data class RoomJoinRuleUpdate(
        override val time: Long,
        val rule: RoomJoinRules
): RoomMessage()

data class RoomHistoryVisibilityUpdate(
        override val time: Long,
        val visibility: HistoryVisibility
): RoomMessage()

data class RoomCanonicalAlias(
        override val time: Long,
        val canonicalAlias: RoomAlias
): RoomMessage()

class ChatMessage(
        val sender: UserState,
        override val time: Long,
        val datetime: Date,
        val content: ChatContent): RoomMessage()
