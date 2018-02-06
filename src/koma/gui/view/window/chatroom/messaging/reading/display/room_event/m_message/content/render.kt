package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.chat.*

fun MRoomMessage.render_node(): ViewNode? {
    val content = this.content
    return when(content) {
        is TextMessage -> MTextViewNode(content)
        is NoticeMessage -> MNoticeViewNode(content)
        is EmoteMessage -> MEmoteViewNode(content, this)
        is ImageMessage -> MImageViewNode(content)
        is FileMessage -> MFileViewNode(content)
        else -> null
    }
}
