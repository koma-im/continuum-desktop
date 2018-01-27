package koma.matrix.event.room_message.chat

sealed class ChatContent() {
    /**
     * text for copying to clipboard
     */
    fun textContent(): String {
        return when(this) {
            is TextMsg -> this.text
            is EmoteMsg -> this.text
            is ImageMsg -> this.mxcurl
            is NoticeMsg -> this.formattedBody
        }
    }
}

class TextMsg(
        val text: String): ChatContent()

data class EmoteMsg(
        val text: String): ChatContent()

data class ImageMsg(
        val desc: String,
        val mxcurl: String): ChatContent()

data class NoticeMsg(
        val formattedBody: String
): ChatContent()

