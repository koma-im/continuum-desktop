package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.site

import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.site.github.GithubView

val siteViewConstructors = mapOf<String, (String) -> ViewNode?>(
        Pair("www.github.com", { link -> GithubView(link) })
)
