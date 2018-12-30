package view

import javafx.geometry.Pos
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Priority
import koma.controller.requests.membership.leaveRoom
import koma.gui.element.icon.AvatarAlways
import koma.gui.view.chatview.SwitchableRoomView
import koma.gui.view.listview.RoomListView
import koma.gui.view.window.chatroom.roominfo.RoomInfoDialog
import koma.storage.config.profile.Profile
import koma.koma_app.appState
import koma.koma_app.appState.apiClient
import model.Room
import model.RoomItemModel
import tornadofx.*


/**
 * Created by developer on 2017/6/21.
 */

class ChatView(profile: Profile): View() {

    override val root = vbox (spacing = 5.0)

    val roomListView: RoomListView
    val switchableRoomView: SwitchableRoomView by inject()

    init {
        val roomList = appState.getAccountRoomStore(profile.userId)!!.roomList
        roomListView = RoomListView(roomList)

        root.addEventFilter(KeyEvent.KEY_PRESSED, { e ->
            if (e.code == KeyCode.PAGE_DOWN) switchableRoomView.scroll(true)
            else if (e.code == KeyCode.PAGE_UP) switchableRoomView.scroll(false)
        })

        with(root) {
            hbox() {
                vgrow = Priority.ALWAYS

                add(roomListView)

                add(switchableRoomView)
            }

        }


    }

}

class RoomFragment: ListCellFragment<Room>() {

    val room = RoomItemModel(itemProperty)
    val iconUrl = room.select { it.iconURLProperty }

    override val root = hbox(spacing = 10.0) {
        minWidth = 1.0
        prefWidth = 1.0
        alignment = Pos.CENTER_LEFT
        contextmenu {
            item("Room Info").action { openInfoView() }
            separator()
            item("Leave").action {
                leaveRoom(itemProperty.value)
            }

        }
        add(AvatarAlways(iconUrl, room.name, room.color))

        label(room.name) {
            textFillProperty().bind(room.color)
        }
    }

    private fun openInfoView() {
        val room = itemProperty.value
        val user = apiClient?.userId
        if (room != null && user != null) {
            RoomInfoDialog(room, user).openWindow()
        }
    }
}


