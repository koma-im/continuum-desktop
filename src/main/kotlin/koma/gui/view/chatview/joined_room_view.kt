package koma.gui.view.chatview

import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import koma.Koma
import koma.gui.view.usersview.RoomMemberListView
import koma.gui.view.window.chatroom.messaging.ChatRecvSendView
import koma.koma_app.AppStore
import link.continuum.desktop.gui.add
import model.Room

/**
 * the room the user is currently interacting with
 * view and send messages, view the list of members in the room
 */
class JoinedRoomView(
        km: Koma,
        store: AppStore
) {
    val root = HBox()

    val messageRecvSendView = ChatRecvSendView(km, store)
    val usersListView = RoomMemberListView(store.userData)

    fun setRoom(room: Room) {
        messageRecvSendView.setRoom(room)
        usersListView.setList(room.members.list)
    }
    init {
        with(root) {
            HBox.setHgrow(this, Priority.ALWAYS)
            add(messageRecvSendView.root)
            add(usersListView.root)
        }
    }
}
