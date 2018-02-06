package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.content

import javafx.scene.Node
import javafx.scene.control.MenuItem
import javafx.scene.text.TextFlow
import koma.gui.view.window.chatroom.messaging.reading.display.ViewNode
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.StringElementTokenizer
import koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview.toNode
import koma.matrix.event.room_message.MRoomMessage
import koma.matrix.event.room_message.chat.EmoteMessage
import koma.matrix.event.room_message.chat.NoticeMessage
import koma.matrix.event.room_message.chat.TextMessage
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
            node.addStringWithElements(content.body)
        }
    }
}

fun TextFlow.addStringWithElements(str: String) {
    val elements =  StringElementTokenizer(str).elements
    this.addNodes(elements.map { it.toNode() })
}

fun TextFlow.addNodes(nodes: List<Node>) {
    nodes.forEach {  this.add(it) }
}
