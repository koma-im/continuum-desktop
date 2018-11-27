package koma.matrix.event.room_message.chat

sealed class M_Message (
    val body: String
) {
    override fun toString(): String {
        return "[m_message: $body]"
    }

}

fun M_Message.getMsgType(): String? {
    val k = when (this) {
        is TextMessage->   "m.text"
        is EmoteMessage->"m.emote"
        is NoticeMessage->"m.notice"
        is ImageMessage->"m.image"
        is FileMessage->  "m.file"
        is LocationMessage->"m.location"
        is VideoMessage->"m.video"
        is AudioMessage->"m.audio"
        is UnrecognizedMessage -> return null
    }
    return  k
}

class TextMessage(
        body: String,
        formatted_body: String?=null,
        val msgtype: String = "m.text",
        format: String?=null
): M_Message(body)

class EmoteMessage(
        body: String,
        val msgtype: String = "m.emote"
): M_Message(body)


class NoticeMessage(
        val msgtype: String = "m.notice",
        body: String
): M_Message(body)

class VideoMessage(
        body: String,
        val msgtype: String = "m.video",
        val url: String,
        val info: VideoInfo?=null
) : M_Message(body)

class AudioMessage(
        body: String,
        val msgtype: String = "m.audio",
        val url: String,
        val info: VideoInfo?=null
) : M_Message(body)

class ImageMessage(
        body: String,
        val url: String,
        val info: ImageInfo?=null,
        val msgtype: String = "m.image"
) : M_Message(body)

class LocationMessage(
        val geo_uri: String,
        val msgtype: String = "m.location",
        val info: LocationInfo?,
        body: String
): M_Message(body)

class FileMessage(
        val filename: String, val url: String, val info: FileInfo? = null,
        val msgtype: String = "m.file",
        body: String = filename
): M_Message(body)

class UnrecognizedMessage(
        val raw: Map<String, Any>,
        body: String = "other"
): M_Message(body)

