package koma.gui.view.listview

import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Priority
import koma.gui.view.RoomFragment
import koma.koma_app.appData
import koma.koma_app.appState
import model.Room
import tornadofx.*

class RoomListView(roomlist: ObservableList<Room>): View() {
    override val root = listview(roomlist)

    init {
        root.isFocusTraversable = false
        root.placeholder = Label("Join a room to start")
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
        val scale = appData.settings.scaling
        node.style {
            fontSize= scale.em
        }
        node.minWidth = 48.0 * scale
        node.maxWidth = 178.0 * scale
        node.vgrow = Priority.ALWAYS
        node.cellFragment(RoomFragment::class)
        node.selectionModel.selectedItemProperty().onChange { room ->
            if (room != null) appState.currRoom.set(room)
        }
   }
}

