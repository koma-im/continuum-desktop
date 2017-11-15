package koma.matrix.event.room_message.chat

import koma.matrix.event.message.ChatMessageType
import koma.matrix.event.room_message.ChatMessage
import koma.matrix.sync.RawMessage
import koma.model.user.UserState
import java.util.*

fun parseChatMessage(sender: UserState, message: RawMessage, content: Map<String, Any>)
        : ChatMessage {
    val msgtype = content["msgtype"] as String?
    val type = msgtype?.let {ChatMessageType.fromString(it)}
    val date = Date(message.origin_server_ts)
    val msg = when (type) {
        ChatMessageType.Text -> ChatMessage(sender, message, date, TextMsg(content["body"] as String? ?: "(unexpected empty text)"))
        ChatMessageType.Emote -> ChatMessage(sender, message, date, EmoteMsg(content["body"] as String? ?: ""))
        ChatMessageType.Image -> ChatMessage(sender, message, date, ImageMsg(content["body"] as String? ?: "",
                mxcurl = content["url"] as String? ?: ""))
        null -> {
            println("message type unavailable $content")
            ChatMessage(sender,message, date, TextMsg("<unexpected null kind of chat message>"))
        }
        else -> ChatMessage(sender, message, date, TextMsg("<unexpected other kind of chat message>"))
    }
    return msg
}
