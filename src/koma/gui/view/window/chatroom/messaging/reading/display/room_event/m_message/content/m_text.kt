package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import javafx.scene.control.MenuItem
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.chat.EmoteMessage
import koma.matrix.event.room_message.chat.TextMessage
import tornadofx.*

class MTextViewNode(val content: TextMessage): ViewNode {
    override val node = TextFlow()
    override val menuItems: List<MenuItem> = listOf()

    init {
        node.add(Text(content.body))
    }
}

class MEmoteViewNode(val content: EmoteMessage, val event: MRoomMessage): ViewNode {
    override val node = TextFlow()
    override val menuItems: List<MenuItem> = listOf()

    init {
        val user = event.sender.getState()
        with(node) {
            label(user.displayName) {
                maxWidth = 100.0
                textFill = user.color
            }
            text(" ")
            text(content.body)
        }
    }
}
