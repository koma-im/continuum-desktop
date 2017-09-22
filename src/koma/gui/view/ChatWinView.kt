package view

import javafx.geometry.Pos
import javafx.scene.layout.Priority
import koma.gui.view.chatview.SwitchableRoomView
import koma.gui.view.listview.RoomListView
import koma.gui.view.roomsview.addMenu
import koma.storage.rooms.UserRoomStore
import model.Room
import model.RoomItemModel
import tornadofx.*


/**
 * Created by developer on 2017/6/21.
 */

class ChatView(): View() {

    override val root = vbox (spacing = 5.0)

    val roomListView: RoomListView
    val switchableRoomView: SwitchableRoomView by inject()

    var selected_room_once = false

    init {
        val roomList = UserRoomStore.roomList
        roomListView = RoomListView(roomList)

        with(root) {

            hbox() {
                vgrow = Priority.ALWAYS

                add(roomListView)

                add(switchableRoomView)
            }

        }

        roomList.onChange {
            if ( !selected_room_once && roomList.isNotEmpty()) {
                roomListView.root.selectionModel.selectFirst()
                selected_room_once = true
            }
        }
    }

}

class RoomFragment: ListCellFragment<Room>() {

    val room = RoomItemModel(itemProperty)

    override val root = hbox(spacing = 10.0) {
        minWidth = 1.0
        prefWidth = 1.0
        alignment = Pos.CENTER_LEFT
        addMenu(this, room.room)
        stackpane {
            imageview(room.icon) {
                isCache = true
                isPreserveRatio = true
            }
            minHeight = 32.0
        }

        // supports ellipses
        label(room.name) {
            textFillProperty().bind(room.color)
        }
    }
}


