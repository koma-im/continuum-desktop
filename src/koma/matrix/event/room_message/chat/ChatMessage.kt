package koma.matrix.event.room_message.chat

sealed class ChatContent()

class TextMsg(
        val text: String): ChatContent() {
    override fun toString(): String {
        return text
    }
}

data class EmoteMsg(
        val text: String): ChatContent()

data class ImageMsg(
        val desc: String,
        val mxcurl: String): ChatContent()
