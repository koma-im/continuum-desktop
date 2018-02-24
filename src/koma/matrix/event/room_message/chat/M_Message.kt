package koma.matrix.event.room_message.chat

open class M_Message (
    val body: String
) {
    override fun toString(): String {
        return "[m_message: $body]"
    }
}

class TextMessage(
        body: String,
        formatted_body: String?=null,
        format: String?=null
): M_Message(body)

class EmoteMessage(
        body: String
): M_Message(body)


class NoticeMessage(
        body: String
): M_Message(body)

class VideoMessage(
        body: String,
        val url: String,
        val info: VideoInfo?=null
) : M_Message(body)

class AudioMessage(
        body: String,
        val url: String,
        val info: VideoInfo?=null
) : M_Message(body)

class ImageMessage(
        body: String,
        val url: String,
        val info: ImageInfo?=null
) : M_Message(body)

class LocationMessage(
        val geo_uri: String,
        val info: LocationInfo?,
        body: String
): M_Message(body)

class FileMessage(
        val filename: String, val url: String, val info: FileInfo? = null,
        body: String = filename
): M_Message(body)

class UnrecognizedMessage(
        body: String
): M_Message(body)

