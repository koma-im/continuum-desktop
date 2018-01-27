package koma.gui.view.listview

import javafx.collections.ObservableList
import javafx.scene.control.ListView
import javafx.scene.control.SelectionModel
import javafx.scene.layout.Priority
import koma.storage.config.settings.AppSettings
import koma_app.appState
import model.Room
import rx.javafx.kt.toObservable
import rx.lang.kotlin.filterNotNull
import tornadofx.*
import view.RoomFragment
import java.util.*

class RoomListView(roomlist: ObservableList<Room>): View() {
    override val root = listview(roomlist)

    init {
        setup(root)
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
                appState.currRoom.set(Optional.of(it))
            }
}


