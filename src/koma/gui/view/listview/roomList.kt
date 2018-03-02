package koma.gui.view.listview

import javafx.collections.ObservableList
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.SelectionModel
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Priority
import koma.storage.config.settings.AppSettings
import koma_app.appState
import model.Room
import rx.javafx.kt.toObservable
import rx.lang.kotlin.filterNotNull
import tornadofx.*
import view.RoomFragment

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
        val scale = AppSettings.settings.scaling
        node.style {
            fontSize= scale.em
        }
        node.minWidth = 48.0 * scale
        node.maxWidth = 178.0 * scale
        node.vgrow = Priority.ALWAYS
        node.cellFragment(RoomFragment::class)
        val selectionModel = node.selectionModel
        switchRoomOnSelect(selectionModel)
   }
}

private fun switchRoomOnSelect(selectionModel: SelectionModel<Room>) {
    selectionModel.selectedItemProperty().toObservable()
            .filterNotNull()
            .subscribe {
                appState.currRoom.set(it)
            }
}


