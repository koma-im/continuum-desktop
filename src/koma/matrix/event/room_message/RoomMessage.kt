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
sealed class RoomMessage()

data class MemberJoinMsg(
        val sender: UserState,
        val datetime: Date,
        val name: String?,
        val avatar_url: String?
): RoomMessage()

data class MemberUpdateMsg(
        val sender: UserState,
        val datetime: Date,
        val name_change: Pair<String?, String?>,
        val avatar_change: Pair<String?, String?>
): RoomMessage()

data class MemberLeave(
        val sender: UserState,
        val datetime: Date
): RoomMessage()

data class MemberBan(
        val sender: UserState,
        val datetime: Date
): RoomMessage()


data class MemberJoin(
        val sender: UserState,
        val datetime: Date
): RoomMessage()

data class RoomCreationMsg(
        val sender: UserState,
        val datetime: Date
        ): RoomMessage() {
    companion object {
        fun fromMessage(message: RawMessage): RoomCreationMsg {
            val sender = UserStore.getOrCreateUserId(message.sender)
            val datetime: Date = Date(message.origin_server_ts)
            val msg = RoomCreationMsg(sender, datetime)
            return msg
        }
    }
}

/**
 * currently only used to update the state of a Room
 */
data class RoomAliasUpdate(
        val aliases: List<RoomAlias>
): RoomMessage()

data class RoomIconUpdate(
        val url: String
): RoomMessage()


data class RoomNameUpdate(
        val name: String
): RoomMessage()


data class RoomGuestAccess(
        val content: Map<String, Any>
): RoomMessage()


data class RoomRedaction(
        val content: Map<String, Any>
): RoomMessage()

data class RoomTopic(
        val content: Map<String, Any>
): RoomMessage()

data class RoomPowerLevel(
        val powerLevels: Map<String, Int>,
        val userLevels: Map<String, Double>
): RoomMessage()

data class RoomJoinRuleUpdate(
        val rule: RoomJoinRules
): RoomMessage()

data class RoomHistoryVisibilityUpdate(
        val visibility: HistoryVisibility
): RoomMessage()

data class RoomCanonicalAlias(
        val canonicalAlias: RoomAlias
): RoomMessage()

class ChatMessage(
        val sender: UserState,
        val datetime: Date,
        val content: ChatContent): RoomMessage()
