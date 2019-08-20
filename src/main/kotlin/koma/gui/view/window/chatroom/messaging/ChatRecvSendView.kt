package koma.gui.view.window.chatroom.messaging

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import koma.Koma
import koma.controller.requests.sendMessage
import koma.gui.view.window.chatroom.messaging.reading.MessagesListScrollPane
import koma.gui.view.window.chatroom.messaging.sending.createButtonBar
import koma.koma_app.AppStore
import koma.matrix.room.naming.RoomId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import link.continuum.desktop.gui.list.user.UserDataStore
import model.Room
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import tornadofx.*

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class ChatRecvSendView(
        km: Koma,
        store: AppStore
): View() {
    override val root = vbox(10.0)

    private val messageScroll = MessagesListScrollPane(km, store)
    private val messageInput = TextField()
    private val currentRoom = SimpleObjectProperty<RoomId>()
    // messages typed but not sent in each room
    private val roomInputs = mutableMapOf<RoomId, String>()

    fun setRoom(room: Room) {
        currentRoom.value?.let {
            roomInputs[it] =messageInput.text
        }
        messageInput.clear()
        roomInputs[room.id]?.let {
            messageInput.appendText(it)
        }
        currentRoom.set(room.id)
        messageScroll.setList(room.messageManager.shownList, room.id)
    }

    init {
        with(root) {
            hgrow = Priority.ALWAYS

            add(messageScroll)

            add(createButtonBar(messageInput, currentRoom))

            add(messageInput)
        }

        with(messageInput) {
            hgrow = Priority.ALWAYS
            action {
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
