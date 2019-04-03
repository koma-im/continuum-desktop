package koma.gui.view.chatview

import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import koma.gui.view.usersview.RoomMemberListView
import koma.gui.view.window.chatroom.messaging.ChatRecvSendView
import kotlinx.coroutines.ObsoleteCoroutinesApi
import model.Room
import okhttp3.HttpUrl
import tornadofx.*

/**
 * the room the user is currently interacting with
 * view and send messages, view the list of members in the room
 */
class JoinedRoomView(room: Room, server: HttpUrl): View() {
    override val root = HBox()

    val messageRecvSendView = ChatRecvSendView(room, server)
    val usersListView = RoomMemberListView(room.members)

    @ObsoleteCoroutinesApi
    fun scroll(down: Boolean) = messageRecvSendView.scroll(down)

    init {
        with(root) {
            hgrow = Priority.ALWAYS
            add(messageRecvSendView)
            add(usersListView)
        }
    }
}
