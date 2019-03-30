package koma.gui.view.listview

import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.ListView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import koma.controller.requests.room.createRoomInteractive
import koma.gui.view.RoomFragment
import koma.gui.view.window.roomfinder.RoomFinder
import koma.koma_app.appState
import koma.storage.persistence.settings.AppSettings
import model.Room
import tornadofx.*

private val settings: AppSettings = appState.store.settings

class RoomListView(roomlist: ObservableList<Room>): View() {
    override val root = listview(roomlist)

    init {
        root.isFocusTraversable = false
        val gettingStarted = VBox(15.0)
        with(gettingStarted) {
            alignment = Pos.CENTER
            hgrow = Priority.ALWAYS
            vgrow = Priority.ALWAYS
            label("Join a room to start")
            button("Find") {
                hgrow= Priority.ALWAYS
                // using a large value to make it as wide as the widest
                maxWidth = 200.0
                action {
                    RoomFinder().open()
                }
            }
            button("Create") {
                hgrow= Priority.ALWAYS
                maxWidth = 200.0
                action {
                    createRoomInteractive()
                }
            }
        }
        root.placeholder = gettingStarted
        root.addEventFilter(KeyEvent.KEY_PRESSED, { e ->
            // these keys are only used to scroll chat messages
            if (e.code == KeyCode.PAGE_UP || e.code == KeyCode.PAGE_DOWN) {
                e.consume()
            }
        })
        setup(root)

        if (roomlist.isNotEmpty()) {
            root.selectionModel.selectFirst()
        }
    }

    private fun setup(node: ListView<Room>) {
        val scale = settings.scaling
        node.style {
            fontSize= scale.em
        }
        node.minWidth = 178.0 * scale
        node.maxWidth = 178.0 * scale
        node.vgrow = Priority.ALWAYS
        node.cellFragment(RoomFragment::class)
        node.selectionModel.selectedItemProperty().onChange { room ->
            if (room != null) appState.currRoom.set(room)
        }
   }
}

