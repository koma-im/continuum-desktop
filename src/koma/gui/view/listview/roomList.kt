package koma.gui.view.listview

import controller.guiEvents
import javafx.scene.control.Alert
import javafx.scene.control.ListView
import javafx.scene.control.MenuItem
import javafx.scene.control.SelectionModel
import javafx.scene.layout.Priority
import koma.storage.rooms.RoomStore
import koma_app.appState
import model.Room
import rx.javafx.kt.actionEvents
import rx.javafx.kt.addTo
import rx.javafx.kt.toObservable
import rx.javafx.sources.CompositeObservable
import rx.lang.kotlin.filterNotNull
import tornadofx.*
import view.RoomFragment
import java.util.*

class RoomListView(): View() {
    override val root = listview(RoomStore.roomList)

    init {
        setup(root)
    }

    private fun setup(node: ListView<Room>) {
        node.vgrow = Priority.ALWAYS
        node.cellFragment(RoomFragment::class)
        val selectionModel = node.selectionModel
        switchRoomOnSelect(selectionModel)

        contextmenu {
            item("leave") {
                requestWithSelected(this, selectionModel, guiEvents.leaveRoomRequests)
            }
            item("upload icon") {
                requestWithSelected(this, selectionModel, guiEvents.uploadRoomIconRequests)
            }
            item("Add alias") {
                requestWithSelected(this, selectionModel, guiEvents.putRoomAliasRequests)
            }
            item("Set alias") {
                requestWithSelected(this, selectionModel, guiEvents.renameRoomRequests)
            }
        }
    }
}

private fun switchRoomOnSelect(selectionModel: SelectionModel<Room>) {
    selectionModel.selectedItemProperty().toObservable()
            .filterNotNull()
            .subscribe {
                appState.currRoom.set(Optional.of(it))
            }
}

private fun requestWithSelected(item: MenuItem,
                                selecttion: SelectionModel<Room>,
                                requests: CompositeObservable<Room>) {
    item.actionEvents().map { selecttion.selectedItem }
            .doOnNext {
                if (it == null)
                    alert(Alert.AlertType.WARNING, "No room selected")
            }
            .filterNotNull()
            .addTo(requests)
}
