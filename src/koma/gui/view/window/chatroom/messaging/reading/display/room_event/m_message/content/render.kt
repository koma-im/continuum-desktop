package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import javafx.scene.Node
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.chat.EmoteMessage
import koma.matrix.event.room_message.chat.FileMessage
import koma.matrix.event.room_message.chat.ImageMessage
import koma.matrix.event.room_message.chat.TextMessage
import tornadofx.*

fun MRoomMessage.render_node(): Node {
    val node = TextFlow()
    val content = this.content
    when(content) {
        is TextMessage -> {
            val text = Text(content.body)
            node.add(text)
        }
        is EmoteMessage -> {
            node.add(Text(content.body))
        }
        is ImageMessage -> return m_image(content)
        is FileMessage -> return m_file(content)
    }
    return node
}
