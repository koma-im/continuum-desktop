package koma.gui.view.window.chatroom.messaging

import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import koma.controller.requests.sendMessage
import koma.gui.view.window.chatroom.messaging.reading.MessagesListScrollPane
import koma.gui.view.window.chatroom.messaging.sending.createButtonBar
import model.Room
import tornadofx.*

class ChatRecvSendView(room: Room): View() {
    override val root = vbox(10.0)

    private val messageScroll = MessagesListScrollPane(room)
    private val messageInput = TextField()

    fun scroll(down: Boolean) = messageScroll.scrollPage(down)
    init {
        with(root) {
            hgrow = Priority.ALWAYS

            add(messageScroll)

            add(createButtonBar(messageInput, room))

            add(messageInput)
        }

        with(messageInput) {
            hgrow = Priority.ALWAYS
            action {
                val msg = text
                text = ""
                if (msg.isNotBlank())
                    sendMessage(room.id, msg)
            }
        }
    }
}
