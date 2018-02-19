package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.site

import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.site.github.createGithubView
import okhttp3.HttpUrl

val siteViewConstructors = mapOf<String, (HttpUrl) -> ViewNode?>(
        Pair("www.github.com", { link -> createGithubView(link) })
)
