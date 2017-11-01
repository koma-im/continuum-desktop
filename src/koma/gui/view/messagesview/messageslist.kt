package koma.gui.view.messagesview

import javafx.collections.ObservableList
import javafx.scene.layout.Priority
import koma.gui.view.MessageListView
import koma.gui.view.messagesview.fragment.MessageCell
import koma.matrix.event.room_message.RoomMessage
import org.fxmisc.flowless.VirtualFlow
import org.fxmisc.flowless.VirtualizedScrollPane
import tornadofx.*

class MessagesScrollListView(messages: ObservableList<RoomMessage>) {
    val listv = MessageListView(messages)
    val root = VirtualizedScrollPane<VirtualFlow<RoomMessage, MessageCell>>(listv.root)
    init {
        root.vgrow = Priority.ALWAYS
    }
}
