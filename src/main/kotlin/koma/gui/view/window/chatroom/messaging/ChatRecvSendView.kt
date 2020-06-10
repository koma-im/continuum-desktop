package koma.gui.view.window.chatroom.messaging

import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Insets
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import koma.controller.requests.sendMessage
import koma.gui.view.window.chatroom.messaging.reading.MessagesListScrollPane
import koma.gui.view.window.chatroom.messaging.sending.createButtonBar
import koma.koma_app.AppStore
import koma.matrix.room.naming.RoomId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import link.continuum.desktop.gui.HBox
import link.continuum.desktop.gui.VBox
import link.continuum.desktop.gui.add
import link.continuum.desktop.gui.view.AccountContext

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class ChatRecvSendView(
        context: AccountContext,
        private val store: AppStore
) {
     val root = VBox(5.0).apply {
        padding = Insets(0.0, 0.0, 5.0, 0.0)
    }

    private val messageScroll = MessagesListScrollPane(context, store)
    private val messageInput = TextField()
    private val currentRoom = SimpleObjectProperty<RoomId>()
    // messages typed but not sent in each room
    private val roomInputs = mutableMapOf<RoomId, String>()

    fun setRoom(room: RoomId) {
        currentRoom.value?.let {
            roomInputs[it] =messageInput.text
        }
        messageInput.clear()
        roomInputs[room]?.let {
            messageInput.appendText(it)
        }
        currentRoom.set(room)
        messageScroll.roomIdObservable.set(value = room)
    }

    init {
        with(root) {
            HBox.setHgrow(this, Priority.ALWAYS)

            add(messageScroll.root)

            add(createButtonBar(messageInput, currentRoom))

            add(messageInput)
        }

        with(messageInput) {
            promptText = "Compose a message"
            HBox.setHgrow(this, Priority.ALWAYS)
            setOnAction {
                val msg = text
                text = ""
                if (msg.isNotBlank()) {
                    currentRoom.value?.let {
                        sendMessage(it, msg)
                    }
                }
            }
        }
    }
}
