package koma.gui.view.window.chatroom.messaging.reading.display.room_event.m_message.embed_preview

import javafx.scene.Node
import javafx.scene.text.Text

fun TextElement.toNode(): Node {
    return when(this.kind) {
        TextElementKind.Plain -> Text(this.text)
        TextElementKind.Link -> hyperlinkNode(this.text)
    }
}

