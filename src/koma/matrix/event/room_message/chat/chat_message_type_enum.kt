package koma.matrix.event.message

import com.squareup.moshi.Json

/**
 * chat message types
 */
enum class ChatMessageType {
    @Json(name="m.text") Text,
    @Json(name="m.emote") Emote,
    @Json(name="m.notice") Notice,
    @Json(name="m.image") Image,
    @Json(name="m.file") File,
    @Json(name="m.location") Location,
    @Json(name="m.video") Video,
    @Json(name="m.audio") Audio,
    Other;

    companion object {
        fun fromString(msgtype: String): ChatMessageType {
            val mtype = when(msgtype) {
                "m.text" -> Text
                "m.emote" -> Emote
                "m.notice" -> Notice
                "m.image" -> Image
                "m.file" -> File
                "m.location" -> Location
                "m.video" -> Video
                "m.audio" -> Audio
                else -> {
                    Other
                }
            }
            return mtype
        }
    }
}
