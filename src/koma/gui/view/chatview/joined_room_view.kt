package koma.gui.view.chatview

import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import koma.gui.view.usersview.RoomMemberListView
import koma.gui.view.window.chatroom.messaging.ChatRecvSendView
import kotlinx.coroutines.ObsoleteCoroutinesApi
import link.continuum.desktop.gui.list.user.UserDataStore
import model.Room
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import tornadofx.*

/**
 * the room the user is currently interacting with
 * view and send messages, view the list of members in the room
 */
class JoinedRoomView(room: Room, server: HttpUrl, userData: UserDataStore,
                     httpClient: OkHttpClient): View() {
    override val root = HBox()

    val messageRecvSendView = ChatRecvSendView(room, server, userData, httpClient)
    val usersListView = RoomMemberListView(room.members, userData, httpClient)

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
