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
    @Json(name="m.audio") Audio;

    override fun toString(): String {
        return when(this) {
            Text -> "m.text"
            Emote -> "m.emote"
            Notice -> "m.notice"
            Image -> "m.image"
            File -> "m.file"
            Location -> "m.location"
            Video -> "m.video"
            Audio -> "m.audio"
        }
    }
}
