package koma.matrix.event.message

enum class MessageType{
    Text,
    Emote,
    Notice,
    Image,
    File,
    Location,
    Video,
    Audio;

    companion object {
        fun fromString(msgtype: String): MessageType {
            val mtype = when(msgtype) {
                "m.text" -> Text
                "m.emote" -> Emote
                "m.notice" -> Notice
                "m.image" -> Image
                "m.file" -> File
                "m.location" -> Location
                "m.video" -> Video
                "m.audio" -> Audio
                else -> throw IllegalArgumentException("unknown kind of message: $msgtype")
            }
            return mtype
        }
    }
}
