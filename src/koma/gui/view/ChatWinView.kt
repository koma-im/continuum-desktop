package view

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory
import gui.view.listview.prettylistview.PrettyListView
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import koma.gui.view.ChatMainView
import koma.gui.view.listview.RoomListView
import koma.gui.view.messagesview.fragment.render_node
import koma.gui.view.roomsview.addMenu
import koma.gui.view.usersview.fragment.get_cell_property
import koma.model.user.UserState
import koma.storage.rooms.RoomStore
import koma_app.appState
import model.MessageItem
import model.MessageItemModel
import model.Room
import model.RoomItemModel
import tornadofx.*


/**
 * Created by developer on 2017/6/21.
 */

class ChatView(): View() {

    override val root = vbox (spacing = 5.0)

    val roomListView: RoomListView by inject()
    val chatMainView: ChatMainView by inject()

    var selected_room_once = false

    init {
        with(root) {

            hbox() {
                vgrow = Priority.ALWAYS

                add(roomListView)

                add(chatMainView)

                vbox(spacing = 10.0) {
                    val showavataronly = SimpleBooleanProperty(true)
                    val expandicon = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.ANGLE_LEFT)
                    val collapseicon = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.ANGLE_RIGHT)
                    val toggleicon = objectBinding(showavataronly) { if (value) expandicon else collapseicon}
                    button {
                        graphicProperty().bind(toggleicon)
                        action {
                            showavataronly.set(showavataronly.not().get())
                        }
                    }
                    val userlist = PrettyListView<UserState>()
                    userlist.apply {
                        itemsProperty().bind(appState.currUserList)
                        vgrow = Priority.ALWAYS
                        minWidth = 50.0
                        val ulwidth = doubleBinding(showavataronly) {if(value) 50.0 else 138.0}
                        maxWidthProperty().bind(ulwidth)
                        prefWidthProperty().bind(ulwidth)
                        cellFactoryProperty().bind(get_cell_property(showavataronly))
                    }
                    add(userlist)
                }

            }

        }

        RoomStore.roomList.addListener { observable, oldValue, newValue ->
            if ( !selected_room_once && newValue.isNotEmpty()) {
                roomListView.root.selectionModel.selectFirst()
                selected_room_once = true
            }
        }
    }

}




class MessageFragment: ListCellFragment<MessageItem>() {

    val msg = MessageItemModel(itemProperty)
    val sender = msg.sender.select { userState -> userState.displayName }
    val avtar = msg.sender.select { us -> us.avatarImgProperty }
    val color = msg.sender.select { us -> us.colorProperty }

    override val root = hbox(spacing = 10.0) {
        minWidth = 1.0
        prefWidth = 1.0
        style {
            alignment = Pos.CENTER_LEFT
        }
        vbox {
            imageview(avtar) {
                isCache = true
                isPreserveRatio = true
            }
            text(msg.date)
        }

        vbox(spacing = 2.0) {
            text(sender) {
                fillProperty().bind(color)
            }

            hbox(spacing = 5.0) {
                msg.message.onChange { it?.render_node()?.let { this.replaceChildren(it) } }
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


