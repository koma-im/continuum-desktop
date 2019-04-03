package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.koma_app.appState
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.chat.*
import okhttp3.HttpUrl

fun MRoomMessage.render_node(server: HttpUrl): ViewNode? {
    val content = this.content
    return when(content) {
        is TextMessage -> MTextViewNode(content)
        is NoticeMessage -> MNoticeViewNode(content)
        is EmoteMessage -> MEmoteViewNode(content, this)
        is ImageMessage -> MImageViewNode(content, server, appState.koma.http.client)
        is FileMessage -> MFileViewNode(content, server)
        else -> null
    }
}
