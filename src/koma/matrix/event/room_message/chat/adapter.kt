package koma.matrix.event.room_message.chat

import com.squareup.moshi.JsonAdapter
import koma.matrix.event.message.ChatMessageType
import koma.matrix.json.RuntimeJsonAdapterFactory

fun getPolyMessageAdapter(): JsonAdapter.Factory {
    val factory = RuntimeJsonAdapterFactory(M_Message::class.java, "msgtype")
    factory.registerSubtype(TextMessage::class.java, ChatMessageType.Text .toString())
    factory.registerSubtype(NoticeMessage::class.java, ChatMessageType.Notice .toString())
    factory.registerSubtype(EmoteMessage::class.java, ChatMessageType.Emote .toString())
    factory.registerSubtype(ImageMessage::class.java, ChatMessageType.Image .toString())
    factory.registerSubtype(FileMessage::class.java, ChatMessageType.File .toString())
    factory.registerDefaultType(UnrecognizedMessage::class.java)
    return factory
}
