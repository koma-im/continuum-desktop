package koma.matrix.event.room_message.chat

import koma.matrix.event.message.ChatMessageType

/**
 * m.room.message messages
 * used only for sending because of moshi's limitations
 */
interface M_Message {
}

class TextMessage(
        val msgtype: ChatMessageType = ChatMessageType.Text,
        val body: String
): M_Message

class ImageMessage(
        val body: String, val url: String, val info: ImageInfo?=null,
        val msgtype:ChatMessageType = ChatMessageType.Image
) : M_Message

class FileMessage(
        val filename: String, val url: String, val info: FileInfo? = null,
        val msgtype: ChatMessageType = ChatMessageType.File,
        val body: String = filename
): M_Message

class ImageInfo(
        val h:Int,
        val w: Int,
        val mimetype: String,
        val size: Int,
        val thumbnail_url: String?,
        val thumbnail_info: ThumbnailInfo?
)

class FileInfo(
        val mimetype: String,
        val size: Int,
        val thumbnail_url: String?,
        val thumbnail_info: ThumbnailInfo?
)

class ThumbnailInfo(
        val h: Int,
        val w: Int,
        val mimetype: String,
        val size: Int
)
