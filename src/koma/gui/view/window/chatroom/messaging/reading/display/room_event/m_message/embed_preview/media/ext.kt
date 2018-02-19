package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.media

import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common.ImageElement
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.common.VideoElement
import okhttp3.HttpUrl


val mediaViewConstructors = constructConstructors()

private fun constructConstructors(): Map<String, (HttpUrl) -> ViewNode?> {
    val map = mutableMapOf<String, (HttpUrl) -> ViewNode?>()

    val imageView = { link: HttpUrl -> ImageElement(link) }
    val imageExts = listOf("jpg", "jpeg", "gif", "png")
    for (i in imageExts) {
        map.put(i, imageView)
    }

    val videoFormats = listOf("mp4")
    for (i in videoFormats) {
        map.put(i, { link: HttpUrl -> VideoElement(link) })
    }

    return map
}
