package model

import koma.matrix.UserId
import koma.matrix.event.message.MessageType
import koma.model.user.UserState
import koma.storage.users.UserStore
import matrix.event.roomevent.RoomEventType
import java.util.*

data class Message(
        val age: Long?,
        val event_id: String,
        val origin_server_ts: Long,
        val prev_content: Map<String, Any>?,
        val type: RoomEventType,
        val sender: UserId,
        val state_key: String?,
        val txn_id: String?,
        val content: Map<String, Any>) {
}


private fun handleMsgTypes(sender: UserState, date: Date, content: Map<String, Any>)
        : MessageFromOthers {
    val msgtype = content["msgtype"] as String?
    val msg = when (MessageType.fromString(msgtype!!)) {
        MessageType.Text -> TextMsg(sender, date,content["body"] as String? ?: "(unexpected empty text)")
        MessageType.Emote -> EmoteMsg(sender,date,content["body"] as String? ?: "")
        MessageType.Image -> ImageMsg(sender, date,content["body"] as String? ?: "",
                mxcurl = content["url"] as String? ?: "")
        else -> TextMsg(sender, date,"(unexpected other message type)")
    }
    return msg
}

/**
 * for display
 * can come from state messages, normal chat messages and others
 */
sealed class MessageToShow

/**
 * normal message from other users
 */
sealed class MessageFromOthers: MessageToShow() {
    abstract val sender: UserState
    abstract val datetime: Date

    companion object {
        fun fromMessage(message: Message): MessageFromOthers? {
            val visible_types = listOf(RoomEventType.Create,
                    RoomEventType.Member,
                    RoomEventType.Message
            )
            if (!visible_types.contains(message.type))
                return null

            val date: Date = java.util.Date(message.origin_server_ts)
            val sender = UserStore.getOrCreateUserId(message.sender)
            val msgShow = when (message.type) {
                RoomEventType.Create -> RoomCreationMsg.fromMessage(message)
                RoomEventType.Member -> {
                    if (message.content["membership"] == "join") {
                        MemberChangeMsg.fromMessage(message)
                    } else
                        TextMsg(sender, date, "left")
                }
                RoomEventType.Message -> handleMsgTypes(sender, date, message.content)
                else -> throw NotImplementedError()
            }
            return msgShow
        }
    }
}

class TextMsg(
        override val sender: UserState,
        override val datetime: Date,
        val text: String): MessageFromOthers()
data class EmoteMsg(
        override val sender: UserState,
        override val datetime: Date,
        val text: String): MessageFromOthers()
data class ImageMsg(
        override val sender: UserState,
        override val datetime: Date,
        val desc: String,
        val mxcurl: String): MessageFromOthers()

sealed class MemberChangeMsg(): MessageFromOthers() {
    companion object {
        fun fromMessage(message: Message): MemberChangeMsg {
            val sender = UserStore.getOrCreateUserId(message.sender)
            val datetime: Date = java.util.Date(message.origin_server_ts)
            val avatar_new = message.content.get("avatar_url") as String?
            val name_new = message.content.get("displayname") as String?

            val notnew = message.prev_content?.get("membership") == "join"
            if (notnew) {
                val avatar_old = message.prev_content?.get("avatar_url") as String?
                val name_old = message.prev_content?.get("displayname") as String?

                return MemberUpdateMsg(sender, datetime,
                        Pair(name_old, name_new), Pair(avatar_old, avatar_new))
            } else {
                return MemberJoinMsg(sender, datetime, name_new, avatar_new)
            }
        }
    }
}

data class MemberJoinMsg(
        override val sender: UserState,
        override val datetime: Date,
        val name: String?,
        val avatar_url: String?
): MemberChangeMsg()

data class MemberUpdateMsg(
        override val sender: UserState,
        override val datetime: Date,
        val name_change: Pair<String?, String?>,
        val avatar_change: Pair<String?, String?>
): MemberChangeMsg()

data class RoomCreationMsg(
        override val sender: UserState,
        override val datetime: Date
        ): MessageFromOthers() {
    companion object {
        fun fromMessage(message: Message): RoomCreationMsg {
            val sender = UserStore.getOrCreateUserId(message.sender)
            val datetime: Date = java.util.Date(message.origin_server_ts)
            val msg = RoomCreationMsg(sender, datetime)
            return msg
        }
    }
}

/**
 * there are messages that aren't loaded yet
 */
class ChatHistoryGap(): MessageToShow()

