package koma.matrix.event.room_message.chat

import koma.matrix.event.message.ChatMessageType
import koma.matrix.event.room_message.ChatMessage
import koma.matrix.sync.RawMessage
import koma.model.user.UserState
import java.util.*

fun parseChatMessage(sender: UserState, message: RawMessage, content: Map<String, Any>)
        : ChatMessage {
    val msgtype = content["msgtype"] as String?
    val type = if (msgtype != null){
        val t = ChatMessageType.fromString(msgtype)
        t
    } else {
        println("message type unavailable $message")
        null
    }
    val date = Date(message.origin_server_ts)
    val msg = when (type) {
        ChatMessageType.Text -> ChatMessage(sender, message, date, TextMsg(content["body"] as String? ?: "(unexpected empty text)"))
        ChatMessageType.Emote -> ChatMessage(sender, message, date, EmoteMsg(content["body"] as String? ?: ""))
        ChatMessageType.Image -> ChatMessage(sender, message, date, ImageMsg(content["body"] as String? ?: "",
                mxcurl = content["url"] as String? ?: ""))
        ChatMessageType.Notice -> {
            val formatted = content["formatted_body"] as String?
            val body = formatted?: content["body"] as String
            ChatMessage(sender, message, date, NoticeMsg(body))
        }
        null -> {
            ChatMessage(sender,message, date, TextMsg("<unexpected null kind of chat message>"))
        }
        else -> {
            println("message type not recognized $message")
            val text = content["body"] as String? ?:"<unexpected other kind of chat message>"
            ChatMessage(sender, message, date, TextMsg(text))
        }
    }
    return msg
}
