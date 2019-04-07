package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import javafx.scene.control.MenuItem
import javafx.scene.text.TextFlow
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.addStringWithElements
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.chat.EmoteMessage
import koma.matrix.event.room_message.chat.NoticeMessage
import koma.matrix.event.room_message.chat.TextMessage
import koma.util.matrix.getState
import tornadofx.*

class MTextViewNode(val content: TextMessage): ViewNode {
    override val node = TextFlow()
    override val menuItems: List<MenuItem> = listOf()

    init {
        node.addStringWithElements(content.body)
    }
}

class MNoticeViewNode(val content: NoticeMessage): ViewNode {
    override val node = TextFlow()
    override val menuItems: List<MenuItem> = listOf()

    init {
        node.addStringWithElements(content.body)
    }
}


class MEmoteViewNode(
        val content: EmoteMessage, val event: MRoomMessage
): ViewNode {
    override val node = TextFlow()
    override val menuItems: List<MenuItem> = listOf()

    init {
        val user = event.sender.getState()
        with(node) {
            label(user.id.str) {
                textFill = user.color
            }
            text(" ")
            node.addStringWithElements(content.body)
        }
    }
}


