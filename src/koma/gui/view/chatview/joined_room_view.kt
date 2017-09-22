package koma.gui.view.chatview

import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import koma.gui.view.ChatRecvSendView
import koma.gui.view.usersview.RoomMemberListView
import model.Room
import tornadofx.*

/**
 * the room the user is currently interacting with
 * view and send messages, view the list of members in the room
 */
class JoinedRoomView(room: Room): View() {
    override val root = HBox()

    val messageRecvSendView = ChatRecvSendView(room)
    val usersListView = RoomMemberListView(room.members)

    init {
        with(root) {
            hgrow = Priority.ALWAYS
            add(messageRecvSendView)
            add(usersListView)
        }
    }
}
