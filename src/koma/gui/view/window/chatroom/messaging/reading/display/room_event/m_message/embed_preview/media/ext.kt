package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.media

import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.media.type.ImageViewNode


val mediaViewConstructors = constructConstructors()

private fun constructConstructors(): Map<String, (String) -> ViewNode?> {
    val map = mutableMapOf<String, (String) -> ViewNode?>()

    val imageView = { link: String -> ImageViewNode(link) }
    val imageExts = listOf("jpg", "jpeg", "gif", "png")
    for (i in imageExts) {
        map.put(i, imageView)
    }

    return map
}
