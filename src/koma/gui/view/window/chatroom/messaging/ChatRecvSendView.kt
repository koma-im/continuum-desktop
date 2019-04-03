package koma.gui.view.window.chatroom.messaging

import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import koma.controller.requests.sendMessage
import koma.gui.view.window.chatroom.messaging.reading.MessagesListScrollPane
import koma.gui.view.window.chatroom.messaging.sending.createButtonBar
import kotlinx.coroutines.ObsoleteCoroutinesApi
import model.Room
import okhttp3.HttpUrl
import tornadofx.*

@ObsoleteCoroutinesApi
class ChatRecvSendView(room: Room, server: HttpUrl): View() {
    override val root = vbox(10.0)

    @ObsoleteCoroutinesApi
    private val messageScroll = MessagesListScrollPane(room, server)
    private val messageInput = TextField()

    @ObsoleteCoroutinesApi
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
