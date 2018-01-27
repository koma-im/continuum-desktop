package view

import javafx.geometry.Pos
import javafx.scene.layout.Priority
import koma.gui.view.chatview.SwitchableRoomView
import koma.gui.view.listview.RoomListView
import koma.gui.view.roomsview.addMenu
import koma.storage.config.profile.Profile
import koma.storage.config.settings.AppSettings
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

    var selected_room_once = false

    init {
        val roomList = profile.getRoomList()
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
        val scale = AppSettings.settings.scaling
        minWidth = 1.0
        prefWidth = 1.0
        alignment = Pos.CENTER_LEFT
        addMenu(this, room.room)
        val iconsize = scale * 32.0
        stackpane {
            imageview(room.icon) {
                isCache = true
                isPreserveRatio = true
            }
            minHeight = iconsize
            minWidth = iconsize
        }

        // supports ellipses
        label(room.name) {
            textFillProperty().bind(room.color)
        }
    }
}


