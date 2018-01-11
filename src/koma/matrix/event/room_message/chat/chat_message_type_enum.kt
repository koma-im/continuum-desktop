package koma.matrix.event.message

/**
 * chat message types
 */
enum class ChatMessageType {
    Text,
    Emote,
    Notice,
    Image,
    File,
    Location,
    Video,
    Audio,
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
