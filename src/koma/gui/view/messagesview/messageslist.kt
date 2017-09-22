package koma.gui.view.messagesview

import javafx.scene.layout.Priority
import koma.gui.view.MessageListView
import koma.gui.view.messagesview.fragment.MessageCell
import model.MessageToShow
import model.Room
import org.fxmisc.flowless.VirtualFlow
import org.fxmisc.flowless.VirtualizedScrollPane
import tornadofx.*

class MessagesScrollListView(room: Room) {
    val listv = MessageListView(room.chatMessages)
    val root = VirtualizedScrollPane<VirtualFlow<MessageToShow, MessageCell>>(listv.root)
    init {
        root.vgrow = Priority.ALWAYS
    }
}
