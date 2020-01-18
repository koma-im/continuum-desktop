package koma.gui.view.listview

import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.ListView
import javafx.scene.layout.Border
import javafx.scene.layout.Priority
import koma.controller.requests.room.createRoomInteractive
import koma.gui.view.RoomFragment
import koma.gui.view.window.roomfinder.RoomFinder
import koma.matrix.room.naming.RoomId
import link.continuum.desktop.database.RoomDataStorage
import link.continuum.desktop.gui.*
import link.continuum.desktop.gui.view.AccountContext

class RoomListView(
        roomlist: ObservableList<RoomId>,
        private val context: AccountContext,
        private val data: RoomDataStorage
) {
    val root = ListView(roomlist)

    init {
        root.border = Border.EMPTY
        root.isFocusTraversable = false
        root.styleClass.add("chatroom-listview")
        val gettingStarted = VBox(15.0)
        with(gettingStarted) {
            alignment = Pos.CENTER
            HBox.setHgrow(this, Priority.ALWAYS)
            VBox.setVgrow(this, Priority.ALWAYS)
            label("Join a room to start")
            button("Find") {
                HBox.setHgrow(this, Priority.ALWAYS)
                // using a large value to make it as wide as the widest
                maxWidth = 200.0
                action {
                    RoomFinder(context.account, data.appData).open()
                }
            }
            button("Create") {
                HBox.setHgrow(this, Priority.ALWAYS)
                maxWidth = 200.0
                action {
                    createRoomInteractive()
                }
            }
        }
        root.placeholder = gettingStarted
        root.style {
            prefWidth = 9.em
        }
        setup(root)
    }

    private fun setup(node: ListView<RoomId>) {
        VBox.setVgrow(node, Priority.ALWAYS)
        node.setCellFactory {
            RoomFragment(data, context)
        }
   }
}

