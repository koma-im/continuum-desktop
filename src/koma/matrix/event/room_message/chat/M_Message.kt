package koma.matrix.event.room_message.chat

import koma.matrix.event.message.ChatMessageType

open class M_Message (
    val body: String,
    /**
     * msgtype needed for sending messages
     */
    val msgtype: ChatMessageType
) {
    override fun toString(): String {
        return "[$msgtype message: $body]"
    }
}

class TextMessage(
        body: String,
        formatted_body: String?=null,
        format: String?=null
): M_Message(body, ChatMessageType.Text)

class EmoteMessage(
        body: String
): M_Message(body, ChatMessageType.Emote)


class NoticeMessage(
        body: String
): M_Message(body, ChatMessageType.Notice)

class VideoMessage(
        body: String,
        val url: String,
        val info: VideoInfo?=null
) : M_Message(body, ChatMessageType.Video)

class AudioMessage(
        body: String,
        val url: String,
        val info: VideoInfo?=null
) : M_Message(body, ChatMessageType.Audio)

class ImageMessage(
        body: String,
        val url: String,
        val info: ImageInfo?=null
) : M_Message(body, ChatMessageType.Image)

class LocationMessage(
        val geo_uri: String,
        val info: LocationInfo?,
        body: String
): M_Message(body, ChatMessageType.Location)

class FileMessage(
        val filename: String, val url: String, val info: FileInfo? = null,
        body: String = filename
): M_Message(body, ChatMessageType.File)

class UnrecognizedMessage(
        body: String
): M_Message(body, ChatMessageType.Unrecognized)

