package koma.gui.view.window.roomfinder

import domain.DiscoveredRoom
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import koma.gui.view.window.roomfinder.publicroomlist.PublicRoomsView
import koma.matrix.publicapi.rooms.getPublicRooms
import koma.storage.config.settings.AppSettings
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.*

class RoomFinder(joinFun: (String)->Unit): Fragment() {
    override val root = VBox(5.0)

    private val publicRoomList: ObservableList<DiscoveredRoom> = FXCollections.observableArrayList<DiscoveredRoom>()
    val pubs: PublicRoomsView
    var stage: Stage? = null

    fun open() {
        stage = this.openWindow()
    }

    init {
        this.title = "Room finder"
        val joinButton = button("Join")
        pubs = PublicRoomsView(publicRoomList, joinButton)
        root.apply {
            this.minWidth = 600.0
            vgrow = Priority.ALWAYS
            style {
                fontSize = AppSettings.scaling.em
            }
            add(pubs.ui)
            buttonbar {
                add(joinButton)
                button("Cancel") {
                    action { stage?.close() }
                }
            }
        }

        joinButton.setDisable(true)
        joinButton.action {
            val rid = this.pubs.roomfield.get()
            joinFun(rid)
        }

        loadPublicRooms()
    }

    private fun loadPublicRooms() {
        val pr = getPublicRooms()
        launch(JavaFx) {
            for (room in pr) {
                publicRoomList.add(room)
                delay(1000)
            }
        }
    }
}
