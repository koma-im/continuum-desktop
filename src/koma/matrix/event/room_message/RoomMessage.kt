package koma.matrix.event.room_message

import koma.matrix.event.room_message.chat.ChatContent
import koma.matrix.room.naming.RoomAlias
import koma.matrix.room.participation.RoomJoinRules
import koma.matrix.room.visibility.HistoryVisibility
import koma.matrix.sync.RawMessage
import koma.model.user.UserState
import koma.storage.users.UserStore
import java.util.*

sealed class RoomMessage(): Comparable<RoomMessage> {
    abstract val original: RawMessage

    override fun compareTo(other: RoomMessage): Int {
        return this.original.origin_server_ts.compareTo(other.original.origin_server_ts)
    }

    override fun equals(other: Any?): Boolean {
        val om = other as RoomMessage
        return this.original.event_id == om.original.event_id
    }

    override fun hashCode() = this.original.event_id.hashCode()
}

class MemberJoinMsg(
        val sender: UserState,
        val datetime: Date,
        override val original: RawMessage,
        val name: String?,
        val avatar_url: String?
): RoomMessage() {
    override fun toString(): String {
        return "<${original.event_id},${original.origin_server_ts},$sender,joins>"
    }
}

class MemberUpdateMsg(
        val sender: UserState,
        val datetime: Date,
        override val original: RawMessage,
        val name_change: Pair<String?, String?>,
        val avatar_change: Pair<String?, String?>
): RoomMessage()

class MemberLeave(
        val sender: UserState,
        override val original: RawMessage,
        val datetime: Date
): RoomMessage()

class MemberBan(
        val sender: UserState,
        override val original: RawMessage,
        val datetime: Date
): RoomMessage()


class MemberJoin(
        val sender: UserState,
        override val original: RawMessage,
        val datetime: Date
): RoomMessage()

class RoomCreationMsg(
        val sender: UserState,
        override val original: RawMessage,
        val datetime: Date
        ): RoomMessage() {
    companion object {
        fun fromMessage(message: RawMessage): RoomCreationMsg {
            val sender = UserStore.getOrCreateUserId(message.sender)
            val datetime: Date = Date(message.origin_server_ts)
            val msg = RoomCreationMsg(sender, message, datetime)
            return msg
        }
    }
}

/**
 * currently only used to update the state of a Room
 */
class RoomAliasUpdate(
        override val original: RawMessage,
        val aliases: List<RoomAlias>
): RoomMessage()

class RoomIconUpdate(
        override val original: RawMessage,
        val url: String
): RoomMessage()


class RoomNameUpdate(
        override val original: RawMessage,
        val name: String
): RoomMessage()


class RoomGuestAccess(
        override val original: RawMessage,
        val content: Map<String, Any>
): RoomMessage()


class RoomRedaction(
        override val original: RawMessage,
        val content: Map<String, Any>
): RoomMessage()

class RoomTopic(
        override val original: RawMessage,
        val content: Map<String, Any>
): RoomMessage()

class RoomPowerLevel(
        override val original: RawMessage,
        val powerLevels: Map<String, Int>,
        val userLevels: Map<String, Double>
): RoomMessage()

class RoomJoinRuleUpdate(
        override val original: RawMessage,
        val rule: RoomJoinRules
): RoomMessage()

class RoomHistoryVisibilityUpdate(
        override val original: RawMessage,
        val visibility: HistoryVisibility
): RoomMessage()

class RoomCanonicalAlias(
        override val original: RawMessage,
        val canonicalAlias: RoomAlias
): RoomMessage()

class ChatMessage(
        val sender: UserState,
        override val original: RawMessage,
        val datetime: Date,
        val content: ChatContent): RoomMessage() {
    override fun toString(): String {
        return "<Chat,$datetime,$content>"
    }
}

class UnrecognizedMessage(
        override val original: RawMessage
): RoomMessage()
