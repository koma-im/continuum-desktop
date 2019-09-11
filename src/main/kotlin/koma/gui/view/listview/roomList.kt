package koma.gui.view.listview

import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.ListView
import javafx.scene.layout.Border
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.controller.requests.room.createRoomInteractive
import koma.gui.view.RoomFragment
import koma.gui.view.window.roomfinder.RoomFinder
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import link.continuum.database.KDataStore
import link.continuum.desktop.gui.action
import link.continuum.desktop.gui.button
import link.continuum.desktop.gui.label
import link.continuum.desktop.util.Account
import model.Room

private val settings: AppSettings = appState.store.settings

class RoomListView(
        roomlist: ObservableList<Room>,
        private val account: Account,
        private val data: KDataStore
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
                    RoomFinder(account).open()
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
        setup(root)
        if (roomlist.isNotEmpty()) {
            root.selectionModel.selectFirst()
        }
    }

    private fun setup(node: ListView<Room>) {
        val scale = settings.scaling
        node.minWidth = 178.0 * scale
        node.maxWidth = 178.0 * scale
        VBox.setVgrow(node, Priority.ALWAYS)
        node.setCellFactory {
            RoomFragment(data, account.server.km)
        }
   }
}

