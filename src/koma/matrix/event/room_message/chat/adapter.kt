package koma.matrix.event.room_message.chat

import com.squareup.moshi.JsonAdapter
import koma.matrix.json.RuntimeJsonAdapterFactory

private val subTypes = mapOf(
        TextMessage::class.java to "m.text",
        EmoteMessage::class.java to "m.emote",
        NoticeMessage::class.java to "m.notice",
        ImageMessage::class.java to "m.image",
        FileMessage::class.java to "m.file",
        LocationMessage::class.java to "m.location",
        VideoMessage::class.java to "m.video",
        AudioMessage::class.java to "m.autio"
)

fun getPolyMessageAdapter(): JsonAdapter.Factory {
    return RuntimeJsonAdapterFactory(
            M_Message::class.java, "msgtype", UnrecognizedMessage::class.java
    ).registerAllSubtypes(subTypes)
}
