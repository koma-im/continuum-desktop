package koma.matrix.event.room_message.chat

fun textToMessage(text: String): M_Message {
    val emoteprefix = "/me "
    if (text.startsWith(emoteprefix)) return EmoteMessage(text.substringAfter(emoteprefix))
    return TextMessage(text)
}
